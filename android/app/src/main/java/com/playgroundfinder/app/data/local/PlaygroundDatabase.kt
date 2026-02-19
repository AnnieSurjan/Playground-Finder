package com.playgroundfinder.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PlaygroundEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PlaygroundDatabase : RoomDatabase() {
    abstract fun playgroundDao(): PlaygroundDao
}
