package com.wledclimb.app.grid

import org.junit.Assert.assertEquals
import org.junit.Test

class WledConfigParserTest {

    // A real /json/cfg response, trimmed to the fields parsePanels() reads.
    private val rawConfig = """
        {"hw":{"led":{"total":147,"matrix":{"mpc":2,"panels":[
            {"b":true,"r":false,"v":true,"s":true,"x":0,"y":0,"h":12,"w":6},
            {"b":true,"r":false,"v":true,"s":true,"x":6,"y":0,"h":12,"w":6}
        ]}}}}
    """.trimIndent()

    @Test
    fun `parses both panels from a real controller's config`() {
        val panels = parsePanels(rawConfig)

        assertEquals(2, panels.size)
        assertEquals(
            Panel(xOffset = 0, yOffset = 0, width = 6, height = 12, bottomStart = true, rightStart = false, vertical = true, serpentine = true),
            panels[0]
        )
        assertEquals(
            Panel(xOffset = 6, yOffset = 0, width = 6, height = 12, bottomStart = true, rightStart = false, vertical = true, serpentine = true),
            panels[1]
        )
    }

    @Test
    fun `parses an empty panels array`() {
        val rawConfigWithNoPanels = """{"hw":{"led":{"matrix":{"mpc":0,"panels":[]}}}}"""

        assertEquals(emptyList<Panel>(), parsePanels(rawConfigWithNoPanels))
    }

    @Test
    fun `parses a gap array`() {
        assertEquals(listOf(1, 1, -1, 0, 1), parseGaps("[1,1,-1,0,1]"))
    }

    @Test
    fun `parses an empty gap array`() {
        assertEquals(emptyList<Int>(), parseGaps("[]"))
    }
}
