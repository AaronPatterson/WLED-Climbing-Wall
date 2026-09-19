package com.wledclimb.app.wled

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
            if (!response.isSuccessful) {
                throw IOException("WLED returned HTTP ${response.code}")
            }
            val body = response.body?.string() ?: throw IOException("Empty response from WLED")
            JSONObject(body).getBoolean("on")
        }
    }

    /** Turns the whole wall on or off. */
    suspend fun setOn(on: Boolean): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("on", on).toString()
        val request = Request.Builder()
            .url("$baseUrl/json/state")
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("WLED returned HTTP ${response.code}")
            }
            val body = response.body?.string() ?: throw IOException("Empty response from WLED")
            JSONObject(body).getBoolean("on")
        }
    }
}
