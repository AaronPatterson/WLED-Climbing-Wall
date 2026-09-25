package com.wledclimb.app.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Saved walls and the routes drawn on them.
 *
 * Foreign keys are enabled explicitly. Room declares the constraint but SQLite
 * ignores foreign keys unless the pragma is set per connection, so without
 * this the cascade that removes a wall's routes would quietly not happen.
 */
@Database(
    entities = [StoredWall::class, StoredRoute::class],
    version = 1,
    exportSchema = true
)
abstract class ClimbDatabase : RoomDatabase() {

    abstract fun walls(): WallDao
    abstract fun routes(): RouteDao

    companion object {
        @Volatile
        private var instance: ClimbDatabase? = null

        /**
         * The one database for the process.
         *
         * Room tolerates several instances over the same file but each opens
         * its own connection and keeps its own invalidation tracker, so a write
         * through one would not wake a Flow collected from another - queries
         * that simply never re-emit, which is a miserable thing to debug.
         */
        fun instance(context: Context): ClimbDatabase =
            instance ?: synchronized(this) {
                instance ?: open(context).also { instance = it }
            }

        fun open(context: Context): ClimbDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                ClimbDatabase::class.java,
                "climb.db"
            ).build()
    }
}
