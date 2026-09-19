package com.wledclimb.app.wled

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Minimal client for a WLED controller's JSON HTTP API.
 *
 * Phase 0 only needs the wall on/off switch, so this wraps exactly one
 * endpoint: GET/POST http://<ip>/json/state. See https://kno.wled.ge/interfaces/json-api/
 * for the full API this will grow into (segments, colors, presets, /json/cfg for the
 * 2D matrix layout, etc.) in later phases.
 */
class WledClient(
    private val baseUrl: String,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {
    private val jsonMediaType = "application/json".toMediaType()

    /** Current power state of the wall, read from WLED. */
    suspend fun getOn(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/json/state")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            parseOn(response)
        }
    }

    /** Turns the whole wall on or off. */
    suspend fun setOn(on: Boolean): Boolean = withContext(Dispatchers.IO) {
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

    private fun parseOn(response: okhttp3.Response): Boolean {
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
