package com.wledclimb.app.storage

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A saved route: which holds are lit and in what colour.
 *
 * [holds] is `x,y:SLOT` pairs separated by semicolons - grid coordinates
 * rather than segment indices, because an index only means something next to
 * the width it was computed with, and palette *positions* rather than colours,
 * so changing the palette does not rewrite every route. See [RouteHolds] and
 * [com.wledclimb.app.grid.GridPosition].
 *
 * [wallFingerprint] is the shape of the wall when this route was last saved.
 * When it stops matching the wall's current fingerprint the route is not
 * deleted or hidden: it is flagged, and opening it shows which of its holds no
 * longer exist so it can be repaired. Losing a route because someone edited a
 * gap file would be much worse than showing a stale one.
 *
 * [readOnly] is not yet surfaced anywhere. It is here now because adding a
 * column later means a migration, and because a route the children have
 * settled on is exactly the kind of thing that gets overwritten by accident.
 *
 * Deleting a wall deletes its routes - they describe positions on that wall
 * and mean nothing without it.
 */
@Entity(
    tableName = "routes",
    foreignKeys = [
        ForeignKey(
            entity = StoredWall::class,
            parentColumns = ["id"],
            childColumns = ["wallId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("wallId")]
)
data class StoredRoute(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wallId: Long,
    val name: String,
    val holds: String,
    val wallFingerprint: String,
    val readOnly: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
