package com.wledclimb.app.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

class HttpWledClientTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client() = HttpWledClient(baseUrl = server.url("").toString().trimEnd('/'))

    @Test
    fun `setBrightness sends the power state so WLED cannot infer it`() = runBlocking {
        // WLED derives power from brightness when "on" is absent
        // (bool on = root["on"] | (bri > 0)), so omitting it would wake a wall
        // that was deliberately switched off.
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"on":false,"bri":100}"""))

        client().setBrightness(brightness = 100, on = false)

        val body = server.takeRequest().body.readUtf8()
        assertTrue("brightness should be sent", body.contains("\"bri\":100"))
        assertTrue("power state should be sent explicitly", body.contains("\"on\":false"))
    }

    @Test
    fun `setBrightness never sends zero`() = runBlocking {
        // Brightness rising from zero is what makes WLED unfreeze every segment
        // and drop the route. The clamp lives in the client rather than relying
        // on every caller and every slider to stay off the boundary.
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"on":true,"bri":8}"""))

        client().setBrightness(brightness = 0, on = true)

        val body = server.takeRequest().body.readUtf8()
        assertTrue("expected a clamped brightness, got: $body", body.contains("\"bri\":8"))
    }

    @Test
    fun `getIdentity reads the controller's name and MAC`() = runBlocking {
        // Trimmed from the real controller's /json/info.
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"ver":"16.0.1","name":"Climbing Wall","mac":"b0cbd8e23458","ip":"192.168.30.49"}"""
            )
        )

        val identity = client().getIdentity()
        assertEquals("Climbing Wall", identity.name)
        assertEquals("b0cbd8e23458", identity.mac)
        assertEquals("/json/info", server.takeRequest().path)
    }

    @Test
    fun `a controller reporting no MAC yields an empty one rather than failing`() = runBlocking {
        // WLED always sends one, but firmware old enough not to should still
        // connect - it falls back to matching the wall on its address.
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"name":"Climbing Wall"}"""))

        val identity = client().getIdentity()
        assertEquals("Climbing Wall", identity.name)
        assertEquals("", identity.mac)
        assertFalse(identity.hasStableId)
    }

    @Test
    fun `getStatus parses power and brightness from a successful response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"on":true,"bri":172}"""))

        val status = client().getStatus()
        assertEquals(true, status.on)
        assertEquals(172, status.brightness)

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/json/state", request.path)
    }

    @Test
    fun `getStatus throws on a non-2xx response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        try {
            client().getStatus()
            fail("Expected an IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("500"))
        }
    }

    @Test
    fun `getStatus throws a descriptive error when the state is the wrong shape`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"unexpected":true}"""))

        try {
            client().getStatus()
            fail("Expected an IOException")
        } catch (e: IOException) {
            // Per the comment in WledClient: the actual payload should be visible
            // in the message so a shape mismatch is obvious without another round trip.
            assertTrue(e.message!!.contains("expected \"on\" and \"bri\""))
            assertTrue(e.message!!.contains("unexpected"))
        }
    }

    @Test
    fun `setOn posts the desired state and parses the confirmed value back`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"on":false,"bri":90}"""))

        val result = client().setOn(on = false)

        assertEquals(false, result.on)
        assertEquals(90, result.brightness)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/json/state", request.path)
        val sentBody = JSONObject(request.body.readUtf8())
        assertEquals(false, sentBody.getBoolean("on"))
        assertEquals(true, sentBody.getBoolean("v"))
    }

    @Test
    fun `getConfig returns the raw response body on success`() = runBlocking {
        val rawConfig = """{"hw":{"led":{"total":1}}}"""
        server.enqueue(MockResponse().setResponseCode(200).setBody(rawConfig))

        assertEquals(rawConfig, client().getConfig())

        val request = server.takeRequest()
        assertEquals("/json/cfg", request.path)
    }

    @Test
    fun `getConfig throws on a non-2xx response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))

        try {
            client().getConfig()
            fail("Expected an IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("404"))
        }
    }

    @Test
    fun `getGaps returns the raw body when the file exists`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[1,1,-1]"))

        assertEquals("[1,1,-1]", client().getGaps())

        val request = server.takeRequest()
        assertEquals("/2d-gaps.json", request.path)
    }

    @Test
    fun `getGaps returns null when WLED reports the file doesn't exist`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))

        assertNull(client().getGaps())
    }

    @Test
    fun `getGaps also returns null for other non-2xx responses, not just 404`() = runBlocking {
        // Documents the current, deliberately lenient behavior: any failure to
        // fetch this optional file is treated as "no gaps" rather than an error,
        // since the wall's grid is still usable without it.
        server.enqueue(MockResponse().setResponseCode(500))

        assertNull(client().getGaps())
    }

    @Test
    fun `setHoldColors blacks out the wall first, then lights the given holds`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true}"""))

        client().setHoldColors(pixelCount = 144, lit = mapOf(5 to "FF0000", 9 to "00FF00"))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/json/state", request.path)

        val individual = JSONObject(request.body.readUtf8())
            .getJSONObject("seg")
            .getJSONArray("i")

        // WLED reads this array in order, so the leading start/stop/colour
        // triple has to clear everything before the per-hold pairs land.
        assertEquals(0, individual.getInt(0))
        assertEquals(144, individual.getInt(1))
        assertEquals("000000", individual.getString(2))
        assertEquals(5, individual.getInt(3))
        assertEquals("FF0000", individual.getString(4))
        assertEquals(9, individual.getInt(5))
        assertEquals("00FF00", individual.getString(6))
        assertEquals(7, individual.length())
    }

    @Test
    fun `setHoldColors with nothing lit still clears the wall`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true}"""))

        client().setHoldColors(pixelCount = 144, lit = emptyMap())

        val individual = JSONObject(server.takeRequest().body.readUtf8())
            .getJSONObject("seg")
            .getJSONArray("i")
        assertEquals(3, individual.length())
        assertEquals("000000", individual.getString(2))
    }

    @Test
    fun `setHoldColors throws on a non-2xx response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        try {
            client().setHoldColors(pixelCount = 144, lit = mapOf(1 to "FF0000"))
            fail("Expected an IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("500"))
        }
    }
}
