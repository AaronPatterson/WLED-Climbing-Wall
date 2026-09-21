package com.wledclimb.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** WLED treats a black pixel as off; there is no separate "off" value. */
private const val OFF_COLOR = "000000"

/**
 * One OkHttpClient for the whole app. Each instance carries its own connection
 * pool and thread pools, and a new client is otherwise built per WledClient -
 * which meant a fresh pool on every "Test & save" tap and every controller
 * switch. Sharing also means connections to the wall get reused.
 */
private val sharedHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
}

/**
 * [WledClient] over WLED's JSON HTTP API.
 *
 * Wraps three endpoints so far: GET/POST http://<ip>/json/state (wall on/off),
 * GET http://<ip>/json/cfg (raw grid/segment config), and GET
 * http://<ip>/2d-gaps.json (optional gap file, served straight from WLED's
 * filesystem). See https://kno.wled.ge/interfaces/json-api/ for the full API
 * this will grow into (colors, presets, etc.) in later phases.
 */
class HttpWledClient(
    private val baseUrl: String,
    private val httpClient: OkHttpClient = sharedHttpClient
) : WledClient {

    private val jsonMediaType = "application/json".toMediaType()

    override suspend fun getOn(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/json/state")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            parseOn(response)
        }
    }

    override suspend fun setOn(on: Boolean): Boolean = withContext(Dispatchers.IO) {
        // WLED's default response to a state-changing POST is just {"success":true};
        // "v":true asks it to reply with the full state instead, matching what GET returns,
        // so parseOn() can handle both the same way.
        val payload = JSONObject().put("on", on).put("v", true).toString()
        val request = Request.Builder()
            .url("$baseUrl/json/state")
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            parseOn(response)
        }
    }

    override suspend fun getConfig(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/json/cfg")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("WLED returned HTTP ${response.code} for ${response.request.url}")
            }
            response.body?.string() ?: throw IOException("Empty response from WLED")
        }
    }

    override suspend fun getGaps(): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/2d-gaps.json")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            response.body?.string()
        }
    }

    override suspend fun setHoldColors(ledCount: Int, lit: Map<Int, String>) =
        withContext(Dispatchers.IO) {
            // WLED walks the "i" array in order, so a start/stop/colour triple
            // blacks out the whole wall first and each index/colour pair after
            // it lights one hold. One request, and the result doesn't depend on
            // what was already showing.
            val individualLeds = JSONArray().apply {
                put(0)
                put(ledCount)
                put(OFF_COLOR)
                lit.forEach { (ledIndex, color) ->
                    put(ledIndex)
                    put(color)
                }
            }
            val payload = JSONObject()
                .put("seg", JSONObject().put("i", individualLeds))
                .toString()
            val request = Request.Builder()
                .url("$baseUrl/json/state")
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("WLED returned HTTP ${response.code} for ${response.request.url}")
                }
            }
        }

    private fun parseOn(response: Response): Boolean {
        if (!response.isSuccessful) {
            throw IOException("WLED returned HTTP ${response.code} for ${response.request.url}")
        }
        val body = response.body?.string() ?: throw IOException("Empty response from WLED")
        return try {
            JSONObject(body).getBoolean("on")
        } catch (e: JSONException) {
            // Surface the actual payload so a shape mismatch (e.g. hitting /json instead
            // of /json/state, or a WLED version returning something unexpected) is obvious
            // from the error message alone, instead of needing another round trip.
            throw IOException(
                "Unexpected response from ${response.request.url} " +
                    "(no \"on\" field): ${body.take(300)}",
                e
            )
        }
    }
}
