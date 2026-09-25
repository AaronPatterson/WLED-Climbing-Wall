package com.wledclimb.app.storage

import com.wledclimb.app.grid.Wall
import com.wledclimb.app.network.WledIdentity

/**
 * The stored wall behind a controller, created the first time one is reached.
 *
 * Routes hang off a wall row, so something has to exist before a route can be
 * saved. That happens on connect rather than on first save: a wall discovered
 * by talking to the controller is a fact, and waiting until someone saves a
 * route would mean doing this work in the middle of the one action that must
 * not fail.
 *
 * **Walls are identified by the controller's MAC**, which WLED reports as
 * `mac` on `/json/info` and takes from the chip's eFuse. It survives reboots,
 * firmware updates, renaming, and a DHCP lease putting the controller on a new
 * address - which the address itself plainly does not. The address is still
 * stored, and updated whenever the controller turns up somewhere new, so a
 * wall can be reached without rediscovery.
 *
 * Address matching remains as a fallback for a controller reporting no MAC,
 * and as the upgrade path for a wall stored before the MAC was read: found by
 * address, its MAC is backfilled, and it is identified properly from then on.
 * A row whose stored MAC disagrees with the one in hand is never reused,
 * because that is a different controller that happens to have been given the
 * same address.
 *
 * Shape and name are refreshed from the controller on every connect, because
 * the controller is the authority on both. [StoredWall.lastSelectedRouteId] is
 * deliberately preserved: it is the app's own state, not the controller's.
 */
class WallRepository(private val walls: WallDao) {

    /** Records which route this wall was last showing, so it can be reselected. */
    suspend fun selectRoute(wallId: Long, routeId: Long?) =
        walls.setLastSelectedRoute(wallId, routeId)

    suspend fun findOrCreate(
        identity: WledIdentity,
        controllerAddress: String,
        wall: Wall
    ): StoredWall {
        val existing = existingFor(identity, controllerAddress)

        if (existing == null) {
            val fresh = StoredWall(
                name = identity.name,
                controllerMac = identity.mac,
                controllerAddress = controllerAddress,
                width = wall.width,
                height = wall.height,
                holdGrid = HoldGrid.serialize(wall)
            )
            return fresh.copy(id = walls.insert(fresh))
        }

        val refreshed = existing.copy(
            name = identity.name,
            // Backfills a wall stored before its MAC was known, and moves the
            // address when the controller turns up somewhere new.
            controllerMac = identity.mac.ifBlank { existing.controllerMac },
            controllerAddress = controllerAddress,
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

    private suspend fun existingFor(
        identity: WledIdentity,
        controllerAddress: String
    ): StoredWall? {
        if (identity.hasStableId) {
            walls.byMac(identity.mac)?.let { return it }
        }

        // Nothing known by that MAC. The address may still lead to this wall -
        // stored before the MAC was read, or by a controller that reports none
        // - but only if it does not already belong to a different controller.
        val byAddress = walls.byAddress(controllerAddress) ?: return null
        val claimedByAnother =
            byAddress.controllerMac.isNotBlank() && byAddress.controllerMac != identity.mac
        return if (claimedByAnother) null else byAddress
    }
}
