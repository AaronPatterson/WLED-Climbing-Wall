package com.wledclimb.app.grid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `horizontal serpentine panel snakes back and forth by row`() {
        // vertical=false means serpentine reverses direction across rows
        // instead of columns - a different branch than the vertical case above.
        val panel = Panel(
            xOffset = 0, yOffset = 0, width = 3, height = 2,
            bottomStart = false, rightStart = false, vertical = false, serpentine = true
        )

        val wall = buildWall(listOf(panel))

        // Row 0: left to right, as usual.
        assertEquals(0, wall.ledIndexAt(x = 0, y = 0))
        assertEquals(1, wall.ledIndexAt(x = 1, y = 0))
        assertEquals(2, wall.ledIndexAt(x = 2, y = 0))

        // Row 1: serpentine reverses it - right to left, continuing from
        // wherever row 0 ended (x=2) rather than jumping back to x=0.
        assertEquals(3, wall.ledIndexAt(x = 2, y = 1))
        assertEquals(4, wall.ledIndexAt(x = 1, y = 1))
        assertEquals(5, wall.ledIndexAt(x = 0, y = 1))
    }

    @Test
    fun `vertical panel without serpentine wires in straight columns`() {
        // vertical=true in isolation, without serpentine - both columns should
        // run the same direction (top to bottom), unlike the serpentine case
        // above where alternating columns reverse.
        val panel = Panel(
            xOffset = 0, yOffset = 0, width = 2, height = 3,
            bottomStart = false, rightStart = false, vertical = true, serpentine = false
        )

        val wall = buildWall(listOf(panel))

        // Column 0: top to bottom.
        assertEquals(0, wall.ledIndexAt(x = 0, y = 0))
        assertEquals(1, wall.ledIndexAt(x = 0, y = 1))
        assertEquals(2, wall.ledIndexAt(x = 0, y = 2))

        // Column 1: also top to bottom, continuing on - no reversal.
        assertEquals(3, wall.ledIndexAt(x = 1, y = 0))
        assertEquals(4, wall.ledIndexAt(x = 1, y = 1))
        assertEquals(5, wall.ledIndexAt(x = 1, y = 2))
    }

    @Test
    fun `rightStart reverses which edge a horizontal panel starts from`() {
        val panel = Panel(
            xOffset = 0, yOffset = 0, width = 3, height = 1,
            bottomStart = false, rightStart = true, vertical = false, serpentine = false
        )

        val wall = buildWall(listOf(panel))

        // LED 0 is at the rightmost column, increasing going left.
        assertEquals(0, wall.ledIndexAt(x = 2, y = 0))
        assertEquals(1, wall.ledIndexAt(x = 1, y = 0))
        assertEquals(2, wall.ledIndexAt(x = 0, y = 0))
    }

    @Test
    fun `bottomStart reverses which edge a horizontal panel's rows start from`() {
        val panel = Panel(
            xOffset = 0, yOffset = 0, width = 1, height = 3,
            bottomStart = true, rightStart = false, vertical = false, serpentine = false
        )

        val wall = buildWall(listOf(panel))

        // LED 0 is at the bottom row, increasing going up.
        assertEquals(0, wall.ledIndexAt(x = 0, y = 2))
        assertEquals(1, wall.ledIndexAt(x = 0, y = 1))
        assertEquals(2, wall.ledIndexAt(x = 0, y = 0))
    }

    @Test
    fun `rightStart on a vertical panel reverses which column is wired first`() {
        // Same shape as "vertical panel without serpentine", but rightStart
        // controls the *column* order here (not the row order, like it did
        // for the horizontal case above) - vertical swaps which flag means what.
        val panel = Panel(
            xOffset = 0, yOffset = 0, width = 2, height = 3,
            bottomStart = false, rightStart = true, vertical = true, serpentine = false
        )

        val wall = buildWall(listOf(panel))

        // Column 1 (rightmost) is wired first now, top to bottom.
        assertEquals(0, wall.ledIndexAt(x = 1, y = 0))
        assertEquals(1, wall.ledIndexAt(x = 1, y = 1))
        assertEquals(2, wall.ledIndexAt(x = 1, y = 2))

        // Column 0 second.
        assertEquals(3, wall.ledIndexAt(x = 0, y = 0))
        assertEquals(4, wall.ledIndexAt(x = 0, y = 1))
        assertEquals(5, wall.ledIndexAt(x = 0, y = 2))
    }

    @Test
    fun `panels with unrelated wiring configs combine correctly`() {
        // A plain horizontal panel next to a plain vertical one - confirms
        // panels are processed independently, not just re-testing one flag
        // combination copy-pasted across panels.
        val horizontal = Panel(
            xOffset = 0, yOffset = 0, width = 2, height = 1,
            bottomStart = false, rightStart = false, vertical = false, serpentine = false
        )
        val vertical = Panel(
            xOffset = 2, yOffset = 0, width = 1, height = 2,
            bottomStart = false, rightStart = false, vertical = true, serpentine = false
        )

        val wall = buildWall(listOf(horizontal, vertical))

        assertEquals(3, wall.width)
        assertEquals(2, wall.height)
        assertEquals(0, wall.ledIndexAt(x = 0, y = 0))
        assertEquals(1, wall.ledIndexAt(x = 1, y = 0))
        assertEquals(2, wall.ledIndexAt(x = 2, y = 0))
        assertEquals(3, wall.ledIndexAt(x = 2, y = 1))
        assertNull(wall.ledIndexAt(x = 0, y = 1))
        assertNull(wall.ledIndexAt(x = 1, y = 1))
    }

    @Test
    fun `no panels produces an empty wall`() {
        val wall = buildWall(emptyList())

        assertEquals(0, wall.width)
        assertEquals(0, wall.height)
        assertNull(wall.ledIndexAt(x = 0, y = 0))
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

    private val threeWideRow = Panel(
        xOffset = 0, yOffset = 0, width = 3, height = 1,
        bottomStart = false, rightStart = false, vertical = false, serpentine = false
    )

    @Test
    fun `a gap value of -1 is unmapped and does not shift later indices`() {
        // Middle cell has no LED at all - the cell after it keeps the index
        // it would have had anyway, as if the missing one was never wired.
        val wall = buildWall(listOf(threeWideRow), gaps = listOf(1, -1, 1))

        assertEquals(0, wall.ledIndexAt(x = 0, y = 0))
        assertNull(wall.ledIndexAt(x = 1, y = 0))
        assertEquals(1, wall.ledIndexAt(x = 2, y = 0))
    }

    @Test
    fun `a gap value of 0 is unmapped but still shifts later indices`() {
        // Middle cell has a real LED wired there, just marked unusable - the
        // cell after it still has to skip over that LED's index.
        val wall = buildWall(listOf(threeWideRow), gaps = listOf(1, 0, 1))

        assertEquals(0, wall.ledIndexAt(x = 0, y = 0))
        assertNull(wall.ledIndexAt(x = 1, y = 0))
        assertEquals(2, wall.ledIndexAt(x = 2, y = 0))
    }

    @Test
    fun `a gap list shorter than the matrix is ignored entirely`() {
        // Matches WLED's own fallback: an incomplete gap file is discarded
        // rather than partially applied.
        val wall = buildWall(listOf(threeWideRow), gaps = listOf(1, -1))

        assertEquals(0, wall.ledIndexAt(x = 0, y = 0))
        assertEquals(1, wall.ledIndexAt(x = 1, y = 0))
        assertEquals(2, wall.ledIndexAt(x = 2, y = 0))
    }

    @Test
    fun `segment index is the grid position, not the position along the strip`() {
        // Regression guard. WLED's per-pixel "i" command writes into the
        // segment's 2D buffer (x + y*width) and applies the ledmap itself when
        // rendering - it does not address the LED's place in the wiring.
        // Sending the wiring index lit a scattered set of the wrong holds on a
        // real wall, because it was being read as a grid position.
        val panel = Panel(
            xOffset = 0, yOffset = 0, width = 6, height = 12,
            bottomStart = true, rightStart = false, vertical = true, serpentine = true
        )
        val wall = buildWall(listOf(panel, panel.copy(xOffset = 6)))

        assertEquals(11, wall.segmentIndexAt(x = 11, y = 0))
        assertEquals(132, wall.segmentIndexAt(x = 0, y = 11))

        // On this wall the two indices are each other's opposite corner, which
        // is exactly why mixing them up mirrored the route.
        assertEquals(132, wall.ledIndexAt(x = 11, y = 0))
        assertEquals(11, wall.segmentIndexAt(x = 11, y = 0))
    }

    @Test
    fun `hasHoldAt reports which cells can be lit`() {
        val panel = Panel(
            xOffset = 0, yOffset = 0, width = 3, height = 1,
            bottomStart = false, rightStart = false, vertical = false, serpentine = false
        )
        val wall = buildWall(listOf(panel), gaps = listOf(1, -1, 1))

        assertTrue(wall.hasHoldAt(x = 0, y = 0))
        assertTrue(!wall.hasHoldAt(x = 1, y = 0))
        assertEquals(3, wall.segmentSize)
    }
}
