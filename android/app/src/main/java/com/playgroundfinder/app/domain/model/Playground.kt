package com.playgroundfinder.app.domain.model

data class Playground(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val rating: Double?,
    val userRatingsTotal: Int?,
    val photoReference: String? = null,
    val isOpenNow: Boolean? = null,
    val isFavorite: Boolean = false,
    val source: PlaygroundSource = PlaygroundSource.GOOGLE
)

enum class PlaygroundSource {
    /** Google Places API eredmény */
    GOOGLE,
    /** OpenStreetMap Overpass API — közösség által jelölt, térképen esetleg nem látható */
    OSM
}
