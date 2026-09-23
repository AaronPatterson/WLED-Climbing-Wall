package com.wledclimb.app.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A climbing wall as the app remembers it, so routes can be opened and edited
 * with no controller in reach.
 *
 * [holdGrid] is the grid, row-major, one character per cell: '1' where a hold
 * can be lit. Stored as text rather than packed bits because a 12x12 wall is
 * 144 characters either way, and one of those two is legible when staring at
 * the database trying to work out why a route looks wrong.
 *
 * Deliberately not named after the gap file it partly comes from. This is the
 * gap file *resolved* against panel coverage and flattened to a yes or no, so
 * it cannot be written back: -1 (no LED wired) and 0 (an LED wired but unused)
 * both land on '0' here, and only the second consumes a strip index. A name
 * suggesting otherwise would invite someone to round-trip it and shift every
 * LED after the first one.
 *
 * [lastSelectedRouteId] lives here rather than in app settings so that it is
 * per wall by construction. Routes belong to the wall they were drawn on and
 * are not meaningfully portable between walls, so there is no sensible global
 * "last route" to keep.
 *
 * [controllerAddress] is recorded so that connecting to a controller can find
 * the wall it belongs to once there is more than one. It is not part of the
 * wall's identity: the same wall reached at a new address is still that wall,
 * which is why the fingerprint ignores it.
 */
@Entity(tableName = "walls")
data class StoredWall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val controllerAddress: String,
    val width: Int,
    val height: Int,
    val holdGrid: String,
    val lastSelectedRouteId: Long? = null
)
