package com.wledclimb.app.wall

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class GridZoomTest {

    private val viewport = Size(width = 1200f, height = 900f)

    @Test
    fun `zoomed right out there is nowhere to pan`() {
        // Otherwise the whole wall could be dragged off screen while still
        // fitting, leaving the user staring at empty space.
        val panned = clampGridPan(Offset(500f, 500f), scale = MIN_GRID_SCALE, viewport = viewport)

        assertEquals(Offset.Zero, panned)
    }

    @Test
    fun `panning is limited to the edges of the zoomed grid`() {
        // At 2x, half the grid is off screen, so it can move by a quarter of
        // its size in each direction before an edge pulls away.
        val max = maxGridPan(scale = 2f, viewport = viewport)

        assertEquals(600f, max.x)
        assertEquals(450f, max.y)
        assertEquals(Offset(600f, -450f), clampGridPan(Offset(9999f, -9999f), 2f, viewport))
    }

    @Test
    fun `panning within the limits is left alone`() {
        val panned = clampGridPan(Offset(100f, -50f), scale = 2f, viewport = viewport)

        assertEquals(Offset(100f, -50f), panned)
    }

    @Test
    fun `zoom cannot go below fitting the whole wall or past the maximum`() {
        assertEquals(MIN_GRID_SCALE, clampGridScale(0.2f))
        assertEquals(MAX_GRID_SCALE, clampGridScale(50f))
        assertEquals(2.5f, clampGridScale(2.5f))
    }
}
