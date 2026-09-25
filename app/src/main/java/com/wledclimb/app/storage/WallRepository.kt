package com.wledclimb.app.storage

import com.wledclimb.app.grid.Wall

/**
 * The stored wall behind a controller, created the first time one is reached.
 *
 * Routes hang off a wall row, so something has to exist before a route can be
 * saved. That happens on connect rather than on first save: a wall discovered
 * by talking to the controller is a fact, and waiting until someone saves a
 * route would mean doing this work in the middle of the one action that must
 * not fail.
 *
 * **Walls are identified by controller address.** That is exact with a
 * hostname or a reserved lease, and wrong under plain DHCP: a controller that
 * comes back on a different address looks like a wall nobody has seen before,
 * so a new row appears and the old wall's routes go with it - not deleted, but
 * attached to a wall the app is no longer looking at. Matching on the wall's
 * shape instead would confuse two identical walls, and matching on name would
 * break the moment one is renamed, so this is the least bad of three
 * imperfect keys rather than a good one. Worth revisiting before anyone has
 * routes they would miss.
 *
 * Shape and name are refreshed from the controller on every connect, because
 * the controller is the authority on both. [StoredWall.lastSelectedRouteId] is
 * deliberately preserved: it is the app's own state, not the controller's.
 */
class WallRepository(private val walls: WallDao) {

    suspend fun findOrCreate(controllerAddress: String, name: String, wall: Wall): StoredWall {
        val existing = walls.byAddress(controllerAddress)

        if (existing == null) {
            val fresh = StoredWall(
                name = name,
                controllerAddress = controllerAddress,
                width = wall.width,
                height = wall.height,
                holdGrid = HoldGrid.serialize(wall)
            )
            return fresh.copy(id = walls.insert(fresh))
        }

        val refreshed = existing.copy(
            name = name,
            width = wall.width,
            height = wall.height,
            holdGrid = HoldGrid.serialize(wall)
        )
        // Only when something actually moved. A write per connect would churn
        // the database for nothing and, once routes are listed from a Flow,
        // re-emit the whole list every time the app reconnects.
        if (refreshed != existing) walls.update(refreshed)
        return refreshed
    }
}
