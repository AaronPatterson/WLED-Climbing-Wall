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

    /** Used when a controller is reached, to find the wall it belongs to. */
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

    @Query("DELETE FROM walls WHERE id = :id")
    suspend fun delete(id: Long)
}
