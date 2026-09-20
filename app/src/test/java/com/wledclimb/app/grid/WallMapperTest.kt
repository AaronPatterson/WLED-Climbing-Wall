package com.wledclimb.app.grid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WallMapperTest {

    @Test
    fun `simple non-serpentine panel maps row-major`() {
        // No flags set: straightforward left-to-right, top-to-bottom wiring.
        val panel = Panel(
            xOffset = 0, yOffset = 0, width = 2, height = 2,
            bottomStart = false, rightStart = false, vertical = false, serpentine = false
        )

        val wall = buildWall(listOf(panel))

        assertEquals(2, wall.width)
        assertEquals(2, wall.height)
        assertEquals(0, wall.ledIndexAt(x = 0, y = 0))
        assertEquals(1, wall.ledIndexAt(x = 1, y = 0))
        assertEquals(2, wall.ledIndexAt(x = 0, y = 1))
        assertEquals(3, wall.ledIndexAt(x = 1, y = 1))
    }

    @Test
    fun `two vertical serpentine panels match a real controller's config`() {
        // From an actual WLED controller's /json/cfg: two 6x12 panels side by
        // side, both wired in vertical, bottom-started, serpentine columns.
        val panel = Panel(
            xOffset = 0, yOffset = 0, width = 6, height = 12,
            bottomStart = true, rightStart = false, vertical = true, serpentine = true
        )
        val panels = listOf(
            panel,
            panel.copy(xOffset = 6)
        )

        val wall = buildWall(panels)

        assertEquals(12, wall.width)
        assertEquals(12, wall.height)

        // Panel 1, column 0 (x=0): bottom-started, so LED 0 is at the bottom
        // and index increases going up.
        assertEquals(0, wall.ledIndexAt(x = 0, y = 11))
        assertEquals(11, wall.ledIndexAt(x = 0, y = 0))

        // Column 1 (x=1): serpentine reverses direction - LED 12 at the top,
        // increasing going down.
        assertEquals(12, wall.ledIndexAt(x = 1, y = 0))
        assertEquals(23, wall.ledIndexAt(x = 1, y = 11))

        // Panel 2 starts numbering right after panel 1's 72 LEDs (6 columns x 12).
        assertEquals(72, wall.ledIndexAt(x = 6, y = 11))
        assertEquals(132, wall.ledIndexAt(x = 11, y = 0))
        assertEquals(143, wall.ledIndexAt(x = 11, y = 11))
    }

    @Test
    fun `cells not covered by any panel are empty`() {
        val panel = Panel(
            xOffset = 0, yOffset = 0, width = 1, height = 1,
            bottomStart = false, rightStart = false, vertical = false, serpentine = false
        )
        // A second panel offset away from the first leaves a gap between them.
        val wall = buildWall(listOf(panel, panel.copy(xOffset = 2, yOffset = 2)))

        assertEquals(0, wall.ledIndexAt(x = 0, y = 0))
        assertEquals(1, wall.ledIndexAt(x = 2, y = 2))
        assertNull(wall.ledIndexAt(x = 1, y = 1))
    }
}
