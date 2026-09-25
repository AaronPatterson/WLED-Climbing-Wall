package com.wledclimb.app.storage

import com.wledclimb.app.grid.Wall
import com.wledclimb.app.grid.fingerprint
import com.wledclimb.app.palette.HoldColor
import kotlinx.coroutines.flow.Flow

/**
 * Saved routes for a wall.
 *
 * Translates between the two ways a route is written down. The app and the
 * wire both work in segment indices; storage works in grid coordinates,
 * because an index only means something beside the width it was computed with
 * (see [com.wledclimb.app.grid.GridPosition]). This class is that boundary,
 * and it is the only place that needs to know both.
 *
 * [now] is injected so tests can assert on timestamps without sleeping.
 */
class RouteRepository(
    private val routes: RouteDao,
    private val now: () -> Long = System::currentTimeMillis
) {

    fun forWall(wallId: Long): Flow<List<StoredRoute>> = routes.forWall(wallId)

    suspend fun byId(id: Long): StoredRoute? = routes.byId(id)

    /**
     * Writes [holds] as a route, creating one when [routeId] is null and
     * overwriting that route when it is not.
     *
     * The wall's fingerprint is recorded as it is now, not as it was when the
     * route was first created: saving is the moment the route is known to
     * match the wall in front of it.
     */
    suspend fun save(
        wallId: Long,
        name: String,
        holds: Map<Int, HoldColor>,
        wall: Wall,
        routeId: Long? = null
    ): Long {
        val stored = RouteHolds.serialize(
            holds.mapKeys { (segmentIndex, _) -> wall.positionOf(segmentIndex) }
        )
        val timestamp = now()

        val existing = routeId?.let { routes.byId(it) }
        if (existing == null) {
            return routes.insert(
                StoredRoute(
                    wallId = wallId,
                    name = name,
                    holds = stored,
                    wallFingerprint = wall.fingerprint,
                    createdAt = timestamp,
                    updatedAt = timestamp
                )
            )
        }

        routes.update(
            existing.copy(
                name = name,
                holds = stored,
                wallFingerprint = wall.fingerprint,
                updatedAt = timestamp
            )
        )
        return existing.id
    }

    /**
     * The route's holds as segment indices, ready to show and push.
     *
     * Holds at positions the wall no longer has are dropped rather than
     * carried through: the wall cannot light a hold that is not there, and
     * pushing an index outside the grid would be a request WLED has no answer
     * for. The route itself is left alone, so the missing holds come back if
     * the wall does - losing a route to a gap-file edit would be far worse
     * than showing one with holes in it.
     */
    suspend fun load(id: Long, wall: Wall): Map<Int, HoldColor>? {
        val route = routes.byId(id) ?: return null
        return RouteHolds.parse(route.holds)
            .filterKeys { wall.hasHoldAt(it.x, it.y) }
            .mapKeys { (position, _) -> wall.segmentIndexAt(position.x, position.y) }
    }

    suspend fun rename(id: Long, name: String) {
        val route = routes.byId(id) ?: return
        routes.update(route.copy(name = name, updatedAt = now()))
    }

    suspend fun delete(id: Long) = routes.delete(id)
}
