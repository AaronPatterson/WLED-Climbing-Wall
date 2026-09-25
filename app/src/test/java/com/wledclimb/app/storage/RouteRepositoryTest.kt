package com.wledclimb.app.storage

import com.wledclimb.app.grid.Wall
import com.wledclimb.app.grid.fingerprint
import com.wledclimb.app.palette.HoldColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteRepositoryTest {

    private val wallId = 1L

    private fun wallOf(vararg rows: String) = Wall(
        width = rows.first().length,
        height = rows.size,
        cells = rows.map { row -> row.map { it == '1' } }
    )

    /** A 3x2 wall with every position occupied, so indices map cleanly. */
    private val wall = wallOf("111", "111")

    private fun repository(dao: RouteDao = InMemoryRouteDao(), clock: () -> Long = { 1000L }) =
        RouteRepository(dao, clock)

    @Test
    fun `a saved route survives the trip through storage`() = runTest {
        val dao = InMemoryRouteDao()
        val repository = repository(dao)
        // Index 4 is (1,1) on a wall three wide - the conversion this class exists for.
        val holds = mapOf(0 to HoldColor.Red, 4 to HoldColor.Blue)

        val id = repository.save(wallId, "Traverse", holds, wall)

        assertEquals(holds, repository.load(id, wall))
    }

    @Test
    fun `routes are stored as coordinates, not segment indices`() = runTest {
        // The reason: index 4 means (1,1) only while the wall is three wide.
        val dao = InMemoryRouteDao()
        val id = repository(dao).save(wallId, "Traverse", mapOf(4 to HoldColor.Blue), wall)

        assertEquals("1,1:4", dao.byId(id)?.holds)
    }

    @Test
    fun `saving over a route keeps its identity and creation time`() = runTest {
        val dao = InMemoryRouteDao()
        var clock = 1000L
        val repository = repository(dao) { clock }

        val id = repository.save(wallId, "Traverse", mapOf(0 to HoldColor.Red), wall)
        clock = 2000L
        val sameId = repository.save(wallId, "Traverse v2", mapOf(1 to HoldColor.Green), wall, routeId = id)

        assertEquals(id, sameId)
        assertEquals(1, dao.forWallCount(wallId))
        val saved = dao.byId(id)!!
        assertEquals("Traverse v2", saved.name)
        assertEquals(1000L, saved.createdAt)
        assertEquals(2000L, saved.updatedAt)
        assertEquals(mapOf(1 to HoldColor.Green), repository.load(id, wall))
    }

    @Test
    fun `saving records the wall as it is now`() = runTest {
        // Not as it was when the route was created: saving is the moment the
        // route is known to match the wall in front of it.
        val dao = InMemoryRouteDao()
        val repository = repository(dao)
        val narrower = wallOf("11", "11")

        val id = repository.save(wallId, "Traverse", mapOf(0 to HoldColor.Red), wall)
        assertEquals(wall.fingerprint, dao.byId(id)?.wallFingerprint)

        repository.save(wallId, "Traverse", mapOf(0 to HoldColor.Red), narrower, routeId = id)
        assertEquals(narrower.fingerprint, dao.byId(id)?.wallFingerprint)
        assertNotEquals(wall.fingerprint, narrower.fingerprint)
    }

    @Test
    fun `loading drops holds the wall no longer has, and keeps the route`() = runTest {
        // A gap-file edit must cost the missing holds, never the whole route -
        // they come back if the wall does.
        val dao = InMemoryRouteDao()
        val repository = repository(dao)
        val id = repository.save(wallId, "Traverse", mapOf(0 to HoldColor.Red, 4 to HoldColor.Blue), wall)

        val gapped = wallOf("111", "101")

        assertEquals(mapOf(0 to HoldColor.Red), repository.load(id, gapped))
        assertEquals("0,0:0;1,1:4", dao.byId(id)?.holds)
    }

    @Test
    fun `loading a route that is not there returns null rather than empty`() = runTest {
        // An empty route and a missing one are different: one clears the wall,
        // the other is a bug or a stale selection.
        assertNull(repository().load(id = 99, wall = wall))
    }

    @Test
    fun `renaming touches the name and the timestamp, not the holds`() = runTest {
        val dao = InMemoryRouteDao()
        var clock = 1000L
        val repository = repository(dao) { clock }
        val id = repository.save(wallId, "Traverse", mapOf(0 to HoldColor.Red), wall)

        clock = 5000L
        repository.rename(id, "Warm up")

        val saved = dao.byId(id)!!
        assertEquals("Warm up", saved.name)
        assertEquals(5000L, saved.updatedAt)
        assertEquals("0,0:0", saved.holds)
    }

    @Test
    fun `deleting removes the route`() = runTest {
        val dao = InMemoryRouteDao()
        val repository = repository(dao)
        val id = repository.save(wallId, "Traverse", mapOf(0 to HoldColor.Red), wall)

        repository.delete(id)

        assertNull(dao.byId(id))
        assertTrue(repository.load(id, wall) == null)
    }
}

private suspend fun RouteDao.forWallCount(wallId: Long): Int = forWall(wallId).first().size
