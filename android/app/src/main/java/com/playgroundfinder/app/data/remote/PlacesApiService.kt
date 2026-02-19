package com.playgroundfinder.app.data.remote

import com.playgroundfinder.app.data.remote.dto.PlaceSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PlacesApiService {

    /**
     * Szöveges keresés a Places API-n.
     * Játszóterek keresésére használjuk (pl. "játszótér Budapest").
     */
    @GET("place/textsearch/json")
    suspend fun searchTextPlaces(
        @Query("query") query: String,
        @Query("location") location: String? = null,  // pl. "47.4979,19.0402"
        @Query("radius") radius: Int? = null,          // méterben, max 50000
        @Query("type") type: String? = null,           // pl. "park", "amusement_park"
        @Query("language") language: String = "hu",    // Magyar eredmények
        @Query("key") apiKey: String
    ): PlaceSearchResponse

    /**
     * Hely részletes adatainak lekérdezése.
     */
    @GET("place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("fields") fields: String = "name,geometry,rating,formatted_address,photos,opening_hours,website,editorial_summary",
        @Query("language") language: String = "hu",
        @Query("key") apiKey: String
    ): PlaceSearchResponse
}
