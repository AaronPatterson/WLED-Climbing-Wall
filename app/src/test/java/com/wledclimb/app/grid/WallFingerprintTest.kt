package com.wledclimb.app.grid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WallFingerprintTest {

    private fun wall(width: Int, height: Int, occupied: (Int, Int) -> Boolean) =
        Wall(width, height, (0 until height).map { y -> (0 until width).map { x -> occupied(x, y) } })

    @Test
    fun `the same shape fingerprints the same every time`() {
        val a = wall(3, 2) { x, _ -> x != 1 }
        val b = wall(3, 2) { x, _ -> x != 1 }

        assertEquals(a.fingerprint, b.fingerprint)
    }

    @Test
    fun `a hold appearing changes the fingerprint`() {
        // The case this exists for: someone edits the gap file and every route
        // built against the old shape needs looking at.
        val before = wall(3, 2) { x, _ -> x != 1 }
        val after = wall(3, 2) { _, _ -> true }

        assertNotEquals(before.fingerprint, after.fingerprint)
    }

    @Test
    fun `the same holds in a different shaped grid fingerprint differently`() {
        // 6x1 and 1x6 hold the same number of holds in the same order, so a
        // fingerprint over occupancy alone would collide.
        assertNotEquals(
            wall(6, 1) { _, _ -> true }.fingerprint,
            wall(1, 6) { _, _ -> true }.fingerprint
        )
    }

    @Test
    fun `an empty wall has a fingerprint rather than blowing up`() {
        assertEquals(16, Wall(0, 0, emptyList()).fingerprint.length)
    }
}
