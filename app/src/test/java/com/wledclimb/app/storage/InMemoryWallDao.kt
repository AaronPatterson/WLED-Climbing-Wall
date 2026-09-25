package com.wledclimb.app.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * [WallDao] backed by a list. Room's own behaviour is covered by WallDaoTest
 * against real SQLite; this exists so callers of the DAO can be tested without
 * standing up a database for each one.
 *
 * Counts writes, because "does not write when nothing changed" is a property
 * worth asserting and is invisible from the stored rows alone.
 */
class InMemoryWallDao : WallDao {

    private val rows = MutableStateFlow<List<StoredWall>>(emptyList())
    private var nextId = 1L

    var insertCount = 0
        private set
    var updateCount = 0
        private set

    override suspend fun byId(id: Long): StoredWall? = rows.value.firstOrNull { it.id == id }

    override suspend fun byAddress(address: String): StoredWall? =
        rows.value.firstOrNull { it.controllerAddress == address }

    override fun all(): Flow<List<StoredWall>> = rows.map { list -> list.sortedBy { it.name } }

    override suspend fun insert(wall: StoredWall): Long {
        insertCount++
        val id = nextId++
        rows.value = rows.value + wall.copy(id = id)
        return id
    }

    override suspend fun update(wall: StoredWall) {
        updateCount++
        rows.value = rows.value.map { if (it.id == wall.id) wall else it }
    }

    override suspend fun setLastSelectedRoute(wallId: Long, routeId: Long?) {
        rows.value = rows.value.map {
            if (it.id == wallId) it.copy(lastSelectedRouteId = routeId) else it
        }
    }

    override suspend fun delete(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}
