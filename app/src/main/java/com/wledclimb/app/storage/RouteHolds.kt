package com.wledclimb.app.storage

import com.wledclimb.app.grid.GridPosition
import com.wledclimb.app.wall.HoldColor

/**
 * Serialises a route's lit holds to and from the text stored on [StoredRoute].
 *
 * Format is `x,y:COLOUR` separated by semicolons, sorted so the same route
 * always produces the same string - which keeps diffs and equality checks
 * honest and makes the column readable when inspecting the database.
 *
 * Colours are stored by enum name. That couples the stored data to those
 * names: renaming `HoldColor.Red` would orphan every route using it. Parsing
 * therefore skips entries it does not recognise rather than throwing, so a
 * route with one unknown colour loses that hold instead of becoming
 * unopenable.
 */
object RouteHolds {

    fun serialize(holds: Map<GridPosition, HoldColor>): String =
        holds.entries
            .sortedWith(compareBy({ it.key.y }, { it.key.x }))
            .joinToString(";") { (position, colour) ->
                "${position.x},${position.y}:${colour.name}"
            }

    fun parse(stored: String): Map<GridPosition, HoldColor> {
        if (stored.isBlank()) return emptyMap()
        return stored.split(';').mapNotNull { entry ->
            val (position, colourName) = entry.split(':', limit = 2)
                .takeIf { it.size == 2 } ?: return@mapNotNull null
            val (x, y) = position.split(',', limit = 2)
                .takeIf { it.size == 2 } ?: return@mapNotNull null
            val colour = HoldColor.entries.firstOrNull { it.name == colourName }
                ?: return@mapNotNull null
            val gx = x.toIntOrNull() ?: return@mapNotNull null
            val gy = y.toIntOrNull() ?: return@mapNotNull null
            GridPosition(gx, gy) to colour
        }.toMap()
    }
}
