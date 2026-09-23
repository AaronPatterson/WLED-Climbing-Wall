package com.wledclimb.app.storage

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteDaoTest : DatabaseTest() {

    @Test
    fun `a route survives a round trip through the database`() = runTest {
        val wallId = walls.insert(wall())

        val id = routes.insert(
            route(wallId, name = "Blue traverse", holds = "0,0:Red;1,1:Blue")
        )

        val stored = routes.byId(id)!!
        assertEquals("Blue traverse", stored.name)
        assertEquals("0,0:Red;1,1:Blue", stored.holds)
        assertEquals(wallId, stored.wallId)
        assertEquals(false, stored.readOnly)
    }

    @Test
    fun `routes are listed most recently edited first`() = runTest {
        val wallId = walls.insert(wall())
        routes.insert(route(wallId, name = "old", updatedAt = 1_000))
        routes.insert(route(wallId, name = "newest", updatedAt = 3_000))
        routes.insert(route(wallId, name = "middle", updatedAt = 2_000))

        assertEquals(
            listOf("newest", "middle", "old"),
            routes.forWall(wallId).first().map { it.name }
        )
    }

    @Test
    fun `a wall's routes are its own`() = runTest {
        val garage = walls.insert(wall(name = "Garage", address = "a"))
        val barn = walls.insert(wall(name = "Barn", address = "b"))
        routes.insert(route(garage, name = "garage route"))
        routes.insert(route(barn, name = "barn route"))

        assertEquals(listOf("garage route"), routes.forWall(garage).first().map { it.name })
    }

    @Test
    fun `deleting a wall deletes its routes`() = runTest {
        // The cascade Room declares only happens because SQLite is told to
        // enforce foreign keys, which it ignores unless the pragma is set per
        // connection. Nothing at compile time checks that it actually fires.
        val wallId = walls.insert(wall())
        val routeId = routes.insert(route(wallId))
        val otherWall = walls.insert(wall(name = "Barn", address = "b"))
        val survivor = routes.insert(route(otherWall))

        walls.delete(wallId)

        assertNull("route should have gone with its wall", routes.byId(routeId))
        assertEquals("other wall's routes untouched", survivor, routes.byId(survivor)?.id)
    }

    @Test
    fun `the route list re-emits when a route is added`() = runTest {
        // A Flow query is the whole reason the list can update itself. If it
        // only emitted once, the screen would silently go stale after a save.
        val wallId = walls.insert(wall())
        val emissions = mutableListOf<List<StoredRoute>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            routes.forWall(wallId).toList(emissions)
        }
        runCurrent()

        routes.insert(route(wallId, name = "added later"))
        runCurrent()
        job.cancel()

        assertEquals("expected an initial emission and one after the insert", 2, emissions.size)
        assertTrue(emissions.first().isEmpty())
        assertEquals(listOf("added later"), emissions.last().map { it.name })
    }

    @Test
    fun `stale routes are those whose wall has changed shape`() = runTest {
        val wallId = walls.insert(wall())
        routes.insert(route(wallId, name = "current", fingerprint = "shape-a"))
        routes.insert(route(wallId, name = "stale", fingerprint = "shape-b"))

        val stale = routes.staleFor(wallId, fingerprint = "shape-a")

        assertEquals(listOf("stale"), stale.map { it.name })
    }

    @Test
    fun `updating a route keeps its id and replaces its holds`() = runTest {
        val wallId = walls.insert(wall())
        val id = routes.insert(route(wallId, holds = "0,0:Red"))

        routes.update(routes.byId(id)!!.copy(holds = "1,1:Blue", updatedAt = 9_000))

        val stored = routes.byId(id)!!
        assertEquals("1,1:Blue", stored.holds)
        assertEquals(9_000, stored.updatedAt)
    }
}
