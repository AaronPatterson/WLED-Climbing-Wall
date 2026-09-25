package com.wledclimb.app.storage

import androidx.room.Entity
import androidx.room.Index
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
 * [draftHolds] is what is on the wall right now when that differs from the
 * route it came from - work in progress that nobody has saved. It is written
 * on every edit so that closing the app, or it being killed in the background,
 * does not throw away a half-built route. Null means the wall matches
 * [lastSelectedRouteId] exactly, or that there is nothing on it.
 *
 * It lives on the wall rather than on a route because a draft need not belong
 * to one: the first route anyone builds is a draft with nothing behind it, and
 * it deserves to survive being interrupted just as much as an edit to a saved
 * route does.
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
 * It is null only if a controller reports no MAC, which WLED goes out of its
 * way to avoid - it falls back to reading eFuse directly rather than
 * publishing zeros. Null rather than an empty string, and that is not a style
 * choice: the column is uniquely indexed, and SQLite treats every NULL in a
 * unique index as distinct from every other while two empty strings collide.
 * Storing "" would therefore let the first unknown-MAC wall be saved and
 * refuse the second. Null means "no usable identity" and is never matched on.
 *
 * WLED also exposes a `deviceId`, and it is deliberately not used: it is a
 * SHA1 of the MAC salted with flash details, exists for WLED's own usage
 * statistics, and is compiled out entirely on ESP-IDF 6 and later. Derived
 * from the MAC, so no more stable, and optional where the MAC is not.
 *
 * The MAC is not the primary key, despite being unique. A wall with no
 * reported MAC has nothing to key on; replacing a controller board changes the
 * MAC for the same physical wall, and re-pointing it should be one column
 * updated rather than a primary key rewritten through every route that
 * references it; and the MAC identifies the *controller*, which is not quite
 * the same thing as the wall. A surrogate id also keeps `routes.wallId` an
 * INTEGER, which SQLite stores as a rowid alias rather than as text repeated
 * in every route row.
 *
 * [controllerAddress] is the last address this controller answered on, kept so
 * a wall can be reached without rediscovery. It is explicitly *not* identity -
 * the same wall at a new address is still that wall, which is the whole reason
 * the MAC is here, and why the fingerprint ignores both.
 */
/*
 * The MAC is uniquely indexed: two rows for one controller would split a
 * wall's routes across both, and whichever the app happened to find first
 * would appear to have lost half of them. Enforced by the database rather
 * than by the repository being careful, because a constraint that lives in
 * one function holds only until someone writes a second one.
 *
 * The MAC is deliberately *not* the primary key - see the note on
 * [controllerMac] for why identity is still a surrogate id.
 */
@Entity(
    tableName = "walls",
    indices = [Index(value = ["controllerMac"], unique = true)]
)
data class StoredWall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val controllerMac: String?,
    val controllerAddress: String,
    val width: Int,
    val height: Int,
    val holdGrid: String,
    val lastSelectedRouteId: Long? = null,
    val draftHolds: String? = null
)
