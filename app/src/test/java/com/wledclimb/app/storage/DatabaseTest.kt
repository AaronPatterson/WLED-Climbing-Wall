package com.wledclimb.app.storage

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Base for tests that run the DAOs against real SQLite on the JVM.
 *
 * Room checks its queries when it compiles them, which catches a misspelled
 * table or a column that does not exist. It does not catch a cascade that
 * never fires, a Flow that does not re-emit, or an ORDER BY that sorts the
 * wrong way - all of which need the query actually run.
 *
 * The database is in memory and rebuilt per test, so nothing leaks between
 * them and there is no file to clean up.
 */
@RunWith(RobolectricTestRunner::class)
abstract class DatabaseTest {

    protected lateinit var db: ClimbDatabase
    protected lateinit var walls: WallDao
    protected lateinit var routes: RouteDao

    @Before
    fun openDatabase() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ClimbDatabase::class.java
        )
            // Run Room's own work on the calling thread. Its Flow queries emit
            // from the invalidation tracker, which normally hops to a
            // background executor that a test scheduler has no way to advance -
            // so a Flow test would see no emissions at all and look like a
            // broken query rather than a scheduling mismatch.
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        walls = db.walls()
        routes = db.routes()
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    protected fun wall(
        name: String = "Garage",
        mac: String = "b0cbd8e23458",
        address: String = "http://192.168.1.50",
        width: Int = 2,
        height: Int = 2,
        holdGrid: String = "1111"
    ) = StoredWall(
        name = name,
        controllerMac = mac,
        controllerAddress = address,
        width = width,
        height = height,
        holdGrid = holdGrid
    )

    protected fun route(
        wallId: Long,
        name: String = "Warm-up",
        holds: String = "0,0:0",
        fingerprint: String = "abc123",
        updatedAt: Long = 1_000
    ) = StoredRoute(
        wallId = wallId,
        name = name,
        holds = holds,
        wallFingerprint = fingerprint,
        createdAt = 1_000,
        updatedAt = updatedAt
    )
}
