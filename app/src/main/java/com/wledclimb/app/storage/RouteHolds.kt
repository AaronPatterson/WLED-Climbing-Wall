package com.wledclimb.app.storage

import com.wledclimb.app.grid.GridPosition
import com.wledclimb.app.grid.Wall
import com.wledclimb.app.palette.HoldColor

/**
 * Serialises a route's lit holds to and from the text stored on [StoredRoute].
 *
 * Format is `x,y:SLOT` separated by semicolons, sorted so the same route
 * always produces the same string - which keeps diffs and equality checks
 * honest.
 *
 * Holds are stored by palette *position*, not by colour - `0,0:1` is "the hold
 * at the origin uses slot 1", and whichever palette is in force decides what
 * slot 1 looks like. Changing or swapping a palette therefore re-skins every
 * saved route with nothing to migrate, and no route can ever reference a
 * colour that no longer exists. The cost is that this column stops being
 * self-describing: reading it by hand needs the palette alongside.
 *
 * Parsing skips entries it cannot make sense of rather than throwing, so a
 * route referencing a slot the current palette does not have loses that hold
 * instead of becoming unopenable. Note this is exactly the case Phase 15 says
 * must not be reached by clamping or wrapping a route into a smaller palette:
 * dropping a hold is recoverable, silently merging two slots onto one colour
 * destroys the distinction the route was built on.
 */
object RouteHolds {

    fun serialize(holds: Map<GridPosition, HoldColor>): String =
        holds.entries
            .sortedWith(compareBy({ it.key.y }, { it.key.x }))
            .joinToString(";") { (position, colour) ->
                "${position.x},${position.y}:${colour.slot}"
            }

    fun parse(stored: String): Map<GridPosition, HoldColor> {
        if (stored.isBlank()) return emptyMap()
        return stored.split(';').mapNotNull { entry ->
            val (position, slot) = entry.split(':', limit = 2)
                .takeIf { it.size == 2 } ?: return@mapNotNull null
            val (x, y) = position.split(',', limit = 2)
                .takeIf { it.size == 2 } ?: return@mapNotNull null
            val colour = slot.toIntOrNull()?.let(HoldColor::atSlot)
                ?: return@mapNotNull null
            val gx = x.toIntOrNull() ?: return@mapNotNull null
            val gy = y.toIntOrNull() ?: return@mapNotNull null
            GridPosition(gx, gy) to colour
        }.toMap()
    }

    /**
     * The same, for holds keyed by segment index.
     *
     * The app and the wire work in indices; storage works in coordinates,
     * because an index only means something beside the width it was computed
     * with. Everything that crosses that line goes through these two, so there
     * is one conversion rather than one per caller.
     */
    fun serializeSegments(holds: Map<Int, HoldColor>, wall: Wall): String =
        serialize(holds.mapKeys { (segmentIndex, _) -> wall.positionOf(segmentIndex) })

    /**
     * Holds at positions [wall] no longer has are dropped. The wall cannot
     * light a hold that is not there, and an index outside the grid is a
     * request WLED has no answer for.
     */
    fun parseSegments(stored: String, wall: Wall): Map<Int, HoldColor> =
        parse(stored)
            .filterKeys { wall.hasHoldAt(it.x, it.y) }
            .mapKeys { (position, _) -> wall.segmentIndexAt(position.x, position.y) }
}
