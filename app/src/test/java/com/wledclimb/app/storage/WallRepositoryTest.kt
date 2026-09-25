package com.wledclimb.app.storage

import com.wledclimb.app.grid.Wall
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WallRepositoryTest {

    private val address = "http://wall.local"

    private fun wallOf(vararg rows: String) = Wall(
        width = rows.first().length,
        height = rows.size,
        cells = rows.map { row -> row.map { it == '1' } }
    )

    @Test
    fun `the first connect stores the wall`() = runTest {
        val dao = InMemoryWallDao()
        val stored = WallRepository(dao).findOrCreate(address, "Garage", wallOf("11", "10"))

        assertEquals(1, dao.insertCount)
        assertEquals("Garage", stored.name)
        assertEquals(address, stored.controllerAddress)
        assertEquals(2, stored.width)
        assertEquals(HoldGrid.serialize(wallOf("11", "10")), stored.holdGrid)
    }

    @Test
    fun `connecting again returns the same row rather than a second wall`() = runTest {
        val dao = InMemoryWallDao()
        val repository = WallRepository(dao)

        val first = repository.findOrCreate(address, "Garage", wallOf("11", "10"))
        val second = repository.findOrCreate(address, "Garage", wallOf("11", "10"))

        assertEquals(first.id, second.id)
        assertEquals(1, dao.insertCount)
    }

    @Test
    fun `an unchanged wall is not rewritten on every connect`() = runTest {
        // Once routes are listed from a Flow, a pointless write per connect
        // re-emits the whole list every time the app reconnects.
        val dao = InMemoryWallDao()
        val repository = WallRepository(dao)

        repository.findOrCreate(address, "Garage", wallOf("11", "10"))
        repository.findOrCreate(address, "Garage", wallOf("11", "10"))

        assertEquals(0, dao.updateCount)
    }

    @Test
    fun `a wall that changed shape is refreshed from the controller`() = runTest {
        val dao = InMemoryWallDao()
        val repository = WallRepository(dao)

        val before = repository.findOrCreate(address, "Garage", wallOf("11", "10"))
        val after = repository.findOrCreate(address, "Barn", wallOf("111", "101"))

        assertEquals(before.id, after.id)
        assertEquals(1, dao.updateCount)
        assertEquals("Barn", after.name)
        assertEquals(3, after.width)
        assertEquals(after, dao.byId(before.id))
    }

    @Test
    fun `refreshing a wall keeps the route the app had selected`() = runTest {
        // The controller is the authority on shape and name. It knows nothing
        // about which route was open, so a refresh must not clear it.
        val dao = InMemoryWallDao()
        val repository = WallRepository(dao)

        val stored = repository.findOrCreate(address, "Garage", wallOf("11", "10"))
        dao.setLastSelectedRoute(stored.id, routeId = 7)

        val refreshed = repository.findOrCreate(address, "Garage", wallOf("111", "101"))

        assertEquals(7L, refreshed.lastSelectedRouteId)
    }
}
