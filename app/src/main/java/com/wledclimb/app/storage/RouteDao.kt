package com.wledclimb.app.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun byId(id: Long): StoredRoute?

    @Query("SELECT * FROM routes WHERE wallId = :wallId ORDER BY updatedAt DESC")
    fun forWall(wallId: Long): Flow<List<StoredRoute>>

    /**
     * Routes whose wall no longer looks the way it did when they were saved.
     * They stay in the list with a warning rather than disappearing.
     */
    @Query("SELECT * FROM routes WHERE wallId = :wallId AND wallFingerprint != :fingerprint")
    suspend fun staleFor(wallId: Long, fingerprint: String): List<StoredRoute>

    @Insert
    suspend fun insert(route: StoredRoute): Long

    @Update
    suspend fun update(route: StoredRoute)

    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun delete(id: Long)
}
