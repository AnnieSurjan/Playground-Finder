package com.playgroundfinder.app.data.remote

import com.playgroundfinder.app.data.remote.dto.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    /**
     * Aktuális időjárás lekérése koordináták alapján.
     * OpenWeatherMap API: https://openweathermap.org/current
     */
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "hu"
    ): WeatherResponse
}
