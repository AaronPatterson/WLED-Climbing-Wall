package com.wledclimb.app.storage

import com.wledclimb.app.grid.GridPosition
import com.wledclimb.app.wall.HoldColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteHoldsTest {

    @Test
    fun `a route survives a round trip`() {
        val holds = mapOf(
            GridPosition(0, 0) to HoldColor.Red,
            GridPosition(3, 4) to HoldColor.Blue,
            GridPosition(11, 11) to HoldColor.Purple
        )

        assertEquals(holds, RouteHolds.parse(RouteHolds.serialize(holds)))
    }

    @Test
    fun `serialisation is stable regardless of insertion order`() {
        // Two routes with the same holds must produce the same string, or
        // equality checks and diffs start reporting changes that are not real.
        val a = mapOf(GridPosition(3, 1) to HoldColor.Red, GridPosition(0, 0) to HoldColor.Blue)
        val b = mapOf(GridPosition(0, 0) to HoldColor.Blue, GridPosition(3, 1) to HoldColor.Red)

        assertEquals(RouteHolds.serialize(a), RouteHolds.serialize(b))
    }

    @Test
    fun `an empty route round trips as empty`() {
        assertEquals(emptyMap<GridPosition, HoldColor>(), RouteHolds.parse(""))
        assertEquals("", RouteHolds.serialize(emptyMap()))
    }

    @Test
    fun `an unknown colour drops that hold rather than the whole route`() {
        // Renaming a HoldColor would orphan stored data. Losing one hold from
        // an otherwise openable route beats a route that cannot be opened.
        val parsed = RouteHolds.parse("0,0:Red;1,1:Chartreuse;2,2:Blue")

        assertEquals(2, parsed.size)
        assertEquals(HoldColor.Red, parsed[GridPosition(0, 0)])
        assertEquals(HoldColor.Blue, parsed[GridPosition(2, 2)])
    }

    @Test
    fun `malformed entries are skipped without throwing`() {
        val parsed = RouteHolds.parse("0,0:Red;garbage;1:Blue;x,y:Green;2,2:Yellow")

        assertEquals(2, parsed.size)
        assertTrue(parsed.containsKey(GridPosition(0, 0)))
        assertTrue(parsed.containsKey(GridPosition(2, 2)))
    }
}
