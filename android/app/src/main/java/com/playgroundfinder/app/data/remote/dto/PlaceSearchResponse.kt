package com.playgroundfinder.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PlaceSearchResponse(
    val results: List<PlaceDto> = emptyList(),
    val status: String = "",
    @SerializedName("next_page_token") val nextPageToken: String? = null,
    // A details API esetén
    val result: PlaceDto? = null
)

data class PlaceDto(
    @SerializedName("place_id") val placeId: String = "",
    val name: String = "",
    val geometry: GeometryDto = GeometryDto(),
    val types: List<String>? = null,
    val rating: Double? = null,
    @SerializedName("user_ratings_total") val userRatingsTotal: Int? = null,
    @SerializedName("formatted_address") val formattedAddress: String? = null,
    val photos: List<PhotoDto>? = null,
    val vicinity: String? = null,
    @SerializedName("opening_hours") val openingHours: OpeningHoursDto? = null,
    val website: String? = null,
    @SerializedName("editorial_summary") val editorialSummary: EditorialSummaryDto? = null
)

data class GeometryDto(
    val location: LocationDto = LocationDto()
)

data class LocationDto(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class PhotoDto(
    @SerializedName("photo_reference") val photoReference: String? = null,
    val height: Int = 0,
    val width: Int = 0
)

data class OpeningHoursDto(
    @SerializedName("open_now") val openNow: Boolean? = null,
    @SerializedName("weekday_text") val weekdayText: List<String>? = null
)

data class EditorialSummaryDto(
    val overview: String? = null
)
