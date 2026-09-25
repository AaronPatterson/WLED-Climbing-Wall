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
 * [controllerMac] is what identifies the wall: WLED's own `mac` from
 * `/json/info`, the WiFi MAC lower-cased with the colons stripped, read from
 * the chip's eFuse. It survives reboots, firmware updates, a DHCP lease moving
 * the controller, and renaming it. Verified against a real controller, which
 * reports `b0cbd8e23458` for this wall.
 *
 * It is empty only if a controller reports no MAC, which WLED goes out of its
 * way to avoid - it falls back to reading eFuse directly rather than publishing
 * zeros. An empty value means "no usable identity", not an identity that
 * several walls share, so it is never matched on.
 *
 * WLED also exposes a `deviceId`, and it is deliberately not used: it is a
 * SHA1 of the MAC salted with flash details, exists for WLED's own usage
 * statistics, and is compiled out entirely on ESP-IDF 6 and later. Derived
 * from the MAC, so no more stable, and optional where the MAC is not.
 *
 * [controllerAddress] is the last address this controller answered on, kept so
 * a wall can be reached without rediscovery. It is explicitly *not* identity -
 * the same wall at a new address is still that wall, which is the whole reason
 * the MAC is here, and why the fingerprint ignores both.
 */
@Entity(tableName = "walls")
data class StoredWall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val controllerMac: String,
    val controllerAddress: String,
    val width: Int,
    val height: Int,
    val holdGrid: String,
    val lastSelectedRouteId: Long? = null
)
