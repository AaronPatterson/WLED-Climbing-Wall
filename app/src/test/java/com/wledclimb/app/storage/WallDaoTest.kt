package com.wledclimb.app.storage

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
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

    @Test
    fun `a wall is found by the controller's MAC`() = runTest {
        walls.insert(wall(name = "Garage", mac = "b0cbd8e23458", address = "a"))
        walls.insert(wall(name = "Barn", mac = "aabbccddeeff", address = "b"))

        assertEquals("Garage", walls.byMac("b0cbd8e23458")?.name)
        assertEquals("Barn", walls.byMac("aabbccddeeff")?.name)
        assertNull(walls.byMac("000000000000"))
    }

    @Test
    fun `two walls cannot share a MAC`() = runTest {
        // Two rows for one controller would split a wall's routes across both,
        // and whichever the app found first would look like it had lost half.
        walls.insert(wall(name = "Garage", mac = "b0cbd8e23458", address = "a"))

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { walls.insert(wall(name = "Copy", mac = "b0cbd8e23458", address = "b")) }
        }
    }

}
