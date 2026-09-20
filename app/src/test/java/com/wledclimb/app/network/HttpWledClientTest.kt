package com.wledclimb.app.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `getOn parses the on field from a successful response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"on":true}"""))

        assertEquals(true, client().getOn())

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/json/state", request.path)
    }

    @Test
    fun `getOn throws on a non-2xx response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        try {
            client().getOn()
            fail("Expected an IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("500"))
        }
    }

    @Test
    fun `getOn throws a descriptive error when the on field is missing`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"unexpected":true}"""))

        try {
            client().getOn()
            fail("Expected an IOException")
        } catch (e: IOException) {
            // Per the comment in WledClient: the actual payload should be visible
            // in the message so a shape mismatch is obvious without another round trip.
            assertTrue(e.message!!.contains("no \"on\" field"))
            assertTrue(e.message!!.contains("unexpected"))
        }
    }

    @Test
    fun `setOn posts the desired state and parses the confirmed value back`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"on":false}"""))

        val result = client().setOn(on = false)

        assertEquals(false, result)
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
}
