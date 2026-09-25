package com.wledclimb.app.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WallDao {

    @Query("SELECT * FROM walls WHERE id = :id")
    suspend fun byId(id: Long): StoredWall?

    /**
     * The wall a controller belongs to. Identity, so this is the lookup that
     * matters - the address one exists only for controllers with no MAC.
     */
    @Query("SELECT * FROM walls WHERE controllerMac = :mac")
    suspend fun byMac(mac: String): StoredWall?

    @Query("SELECT * FROM walls WHERE controllerAddress = :address LIMIT 1")
    suspend fun byAddress(address: String): StoredWall?

    @Query("SELECT * FROM walls ORDER BY name")
    fun all(): Flow<List<StoredWall>>

    @Insert
    suspend fun insert(wall: StoredWall): Long

    @Update
    suspend fun update(wall: StoredWall)

    /**
     * Narrow rather than a whole-row update: this is written every time a route
     * is picked, and a full update would race with an edit to the wall's shape
     * happening at the same moment.
     */
    @Query("UPDATE walls SET lastSelectedRouteId = :routeId WHERE id = :wallId")
    suspend fun setLastSelectedRoute(wallId: Long, routeId: Long?)

    /**
     * Narrow like [setLastSelectedRoute], and for the same reason: this is
     * written on every hold tap, and a whole-row update would race with the
     * wall's shape being refreshed on a reconnect.
     */
    @Query("UPDATE walls SET draftHolds = :draftHolds WHERE id = :wallId")
    suspend fun setDraft(wallId: Long, draftHolds: String?)

    @Query("DELETE FROM walls WHERE id = :id")
    suspend fun delete(id: Long)
}
