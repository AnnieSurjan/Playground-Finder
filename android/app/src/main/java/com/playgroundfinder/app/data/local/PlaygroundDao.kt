package com.playgroundfinder.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaygroundDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayground(playground: PlaygroundEntity)

    @Delete
    suspend fun deletePlayground(playground: PlaygroundEntity)

    @Query("SELECT * FROM favorite_playgrounds ORDER BY savedAt DESC")
    fun getFavoritePlaygrounds(): Flow<List<PlaygroundEntity>>

    @Query("SELECT * FROM favorite_playgrounds WHERE id = :id")
    suspend fun getFavoritePlaygroundById(id: String): PlaygroundEntity?

    @Query("DELETE FROM favorite_playgrounds WHERE id = :id")
    suspend fun deleteFavoriteById(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_playgrounds WHERE id = :id)")
    suspend fun isFavorite(id: String): Boolean
}
