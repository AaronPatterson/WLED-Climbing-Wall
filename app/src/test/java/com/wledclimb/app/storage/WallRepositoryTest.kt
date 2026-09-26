package com.wledclimb.app.storage

import com.wledclimb.app.grid.Wall
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WallRepositoryTest {

    private val address = "http://wall.local"
    private val mac = "b0cbd8e23458"

    /** What a connect hands the repository: a MAC, a name, an address, a shape. */
    private suspend fun WallRepository.reached(
        wall: Wall,
        name: String = "Garage",
        mac: String? = this@WallRepositoryTest.mac,
        address: String = this@WallRepositoryTest.address
    ) = findOrCreate(controllerMac = mac, name = name, controllerAddress = address, wall = wall)

    private fun wallOf(vararg rows: String) = Wall(
        width = rows.first().length,
        height = rows.size,
        cells = rows.map { row -> row.map { it == '1' } }
    )

    @Test
    fun `the first connect stores the wall`() = runTest {
        val dao = InMemoryWallDao()
        val stored = WallRepository(dao).reached(wallOf("11", "10"))

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

        val first = repository.reached(wallOf("11", "10"))
        val second = repository.reached(wallOf("11", "10"))

        assertEquals(first.id, second.id)
        assertEquals(1, dao.insertCount)
    }

    @Test
    fun `an unchanged wall is not rewritten on every connect`() = runTest {
        // Once routes are listed from a Flow, a pointless write per connect
        // re-emits the whole list every time the app reconnects.
        val dao = InMemoryWallDao()
        val repository = WallRepository(dao)

        repository.reached(wallOf("11", "10"))
        repository.reached(wallOf("11", "10"))

        assertEquals(0, dao.updateCount)
    }

    @Test
    fun `a wall that changed shape is refreshed from the controller`() = runTest {
        val dao = InMemoryWallDao()
        val repository = WallRepository(dao)

        val before = repository.reached(wallOf("11", "10"))
        val after = repository.reached(wallOf("111", "101"), name = "Barn")

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

        val stored = repository.reached(wallOf("11", "10"))
        dao.setLastSelectedRoute(stored.id, routeId = 7)

        val refreshed = repository.reached(wallOf("111", "101"))

        assertEquals(7L, refreshed.lastSelectedRouteId)
    }

    @Test
    fun `a controller that moved to a new address is the same wall`() = runTest {
        // The reason the MAC is stored at all: a DHCP lease must not orphan
        // every route drawn on the wall.
        val dao = InMemoryWallDao()
        val repository = WallRepository(dao)

        val before = repository.reached(wallOf("11", "10"))
        val after = repository.reached(wallOf("11", "10"), address = "http://192.168.30.77")

        assertEquals(before.id, after.id)
        assertEquals(1, dao.insertCount)
        assertEquals("http://192.168.30.77", after.controllerAddress)
    }

    @Test
    fun `a different controller on a reused address is a different wall`() = runTest {
        // The other half: an address says nothing about identity, so a lease
        // handed to someone else must not adopt the previous wall's routes.
        val dao = InMemoryWallDao()
        val repository = WallRepository(dao)

        val first = repository.reached(wallOf("11", "10"))
        val second = repository.reached(wallOf("11", "10"), name = "Barn", mac = "aabbccddeeff")

        assertNotEquals(first.id, second.id)
        assertEquals(2, dao.insertCount)
    }

    @Test
    fun `a wall stored before its MAC was known is adopted, not duplicated`() = runTest {
        // The upgrade path. Found by address because no MAC was recorded, then
        // identified properly from then on.
        val dao = InMemoryWallDao()
        val repository = WallRepository(dao)
        dao.insert(
            StoredWall(
                name = "Garage",
                controllerMac = null,
                controllerAddress = address,
                width = 2,
                height = 2,
                holdGrid = HoldGrid.serialize(wallOf("11", "10"))
            )
        )

        val adopted = repository.reached(wallOf("11", "10"))

        assertEquals(1, dao.insertCount)
        assertEquals(mac, adopted.controllerMac)
        assertEquals(adopted.id, dao.byMac(mac)?.id)
    }

    @Test
    fun `a controller reporting no MAC falls back to matching on address`() = runTest {
        val dao = InMemoryWallDao()
        val repository = WallRepository(dao)
        val first = repository.reached(wallOf("11", "10"), mac = null)
        val second = repository.reached(wallOf("11", "10"), mac = null)

        assertEquals(first.id, second.id)
        assertEquals(1, dao.insertCount)
        assertNull(second.controllerMac)
    }
}
