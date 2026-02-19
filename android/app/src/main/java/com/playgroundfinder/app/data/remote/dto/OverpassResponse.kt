package com.playgroundfinder.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OverpassResponse(
    val elements: List<OverpassElement> = emptyList()
)

data class OverpassElement(
    val type: String,            // "node" vagy "way"
    val id: Long,
    val lat: Double? = null,     // node esetén közvetlen
    val lon: Double? = null,
    val center: OverpassCenter? = null,  // way esetén
    val tags: Map<String, String>? = null
) {
    val centerLat: Double? get() = lat ?: center?.lat
    val centerLon: Double? get() = lon ?: center?.lon
}

data class OverpassCenter(
    val lat: Double,
    val lon: Double
)
