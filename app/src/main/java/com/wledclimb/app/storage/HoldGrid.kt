package com.wledclimb.app.storage

import com.wledclimb.app.grid.Wall

/**
 * Converts a wall's grid to and from the row-major text on [StoredWall].
 *
 * A stored string whose length disagrees with the dimensions is treated as
 * unusable rather than padded: a half-read grid would put holds in the wrong
 * places, which is worse than admitting the wall needs fetching again.
 */
object HoldGrid {

    fun serialize(wall: Wall): String = buildString {
        for (row in wall.cells) for (cell in row) append(if (cell) '1' else '0')
    }

    fun parse(holdGrid: String, width: Int, height: Int): Wall? {
        if (width < 0 || height < 0) return null
        if (holdGrid.length != width * height) return null
        val cells = (0 until height).map { y ->
            (0 until width).map { x -> holdGrid[y * width + x] == '1' }
        }
        return Wall(width = width, height = height, cells = cells)
    }
}
