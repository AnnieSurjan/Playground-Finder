package com.playgroundfinder.app.di

import android.app.Application
import androidx.room.Room
import com.playgroundfinder.app.data.local.PlaygroundDao
import com.playgroundfinder.app.data.local.PlaygroundDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePlaygroundDatabase(app: Application): PlaygroundDatabase {
        return Room.databaseBuilder(
            app,
            PlaygroundDatabase::class.java,
            "playground_db"
        ).build()
    }

    @Provides
    @Singleton
    fun providePlaygroundDao(db: PlaygroundDatabase): PlaygroundDao {
        return db.playgroundDao()
    }
}
