package com.wledclimb.app.storage

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WallDaoTest : DatabaseTest() {

    @Test
    fun `a wall survives a round trip through the database`() = runTest {
        val id = walls.insert(wall(name = "Garage", holdGrid = "1011"))

        val stored = walls.byId(id)!!
        assertEquals("Garage", stored.name)
        assertEquals("1011", stored.holdGrid)
        assertEquals(id, stored.id)
    }

    @Test
    fun `a wall is found by the controller it was reached at`() = runTest {
        walls.insert(wall(name = "Garage", address = "http://192.168.1.50"))
        walls.insert(wall(name = "Barn", address = "http://192.168.1.60"))

        assertEquals("Barn", walls.byAddress("http://192.168.1.60")?.name)
        assertNull(walls.byAddress("http://192.168.1.99"))
    }

    @Test
    fun `walls are listed by name`() = runTest {
        walls.insert(wall(name = "Shed", address = "a"))
        walls.insert(wall(name = "Barn", address = "b"))
        walls.insert(wall(name = "Garage", address = "c"))

        assertEquals(listOf("Barn", "Garage", "Shed"), walls.all().first().map { it.name })
    }

    @Test
    fun `setting the last selected route leaves the rest of the wall alone`() = runTest {
        // The narrow UPDATE exists so that picking a route cannot clobber a
        // change to the wall's shape made at the same moment.
        val wallId = walls.insert(wall(name = "Garage", holdGrid = "1111"))
        val routeId = routes.insert(route(wallId))

        walls.setLastSelectedRoute(wallId, routeId)

        val stored = walls.byId(wallId)!!
        assertEquals(routeId, stored.lastSelectedRouteId)
        assertEquals("Garage", stored.name)
        assertEquals("1111", stored.holdGrid)
    }

    @Test
    fun `the last selected route can be cleared`() = runTest {
        val wallId = walls.insert(wall())
        val routeId = routes.insert(route(wallId))
        walls.setLastSelectedRoute(wallId, routeId)

        walls.setLastSelectedRoute(wallId, null)

        assertNull(walls.byId(wallId)!!.lastSelectedRouteId)
    }
}
