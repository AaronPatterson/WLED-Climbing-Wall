package com.wledclimb.app.storage

import com.wledclimb.app.grid.Wall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WallOccupancyTest {

    private val wall = Wall(
        width = 3,
        height = 2,
        cells = listOf(
            listOf(true, false, true),
            listOf(false, true, false)
        )
    )

    @Test
    fun `a wall survives a round trip`() {
        val stored = HoldGrid.serialize(wall)

        assertEquals("101010", stored)
        assertEquals(wall, HoldGrid.parse(stored, width = 3, height = 2))
    }

    @Test
    fun `holdGrid is written row-major`() {
        val parsed = HoldGrid.parse("101010", width = 3, height = 2)!!

        assertTrue(parsed.hasHoldAt(x = 0, y = 0))
        assertFalse(parsed.hasHoldAt(x = 1, y = 0))
        assertTrue(parsed.hasHoldAt(x = 1, y = 1))
    }

    @Test
    fun `a length that disagrees with the dimensions is rejected`() {
        // Padding or truncating would put holds in the wrong places, which is
        // worse than admitting the wall needs fetching from the controller.
        assertNull(HoldGrid.parse("10101", width = 3, height = 2))
        assertNull(HoldGrid.parse("1010101", width = 3, height = 2))
    }

    @Test
    fun `an empty wall round trips`() {
        val empty = Wall(0, 0, emptyList())

        assertEquals("", HoldGrid.serialize(empty))
        assertEquals(empty, HoldGrid.parse("", width = 0, height = 0))
    }
}
