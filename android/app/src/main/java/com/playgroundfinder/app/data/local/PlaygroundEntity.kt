package com.playgroundfinder.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_playgrounds")
data class PlaygroundEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val rating: Double?,
    val userRatingsTotal: Int?,
    val savedAt: Long = System.currentTimeMillis()
)
