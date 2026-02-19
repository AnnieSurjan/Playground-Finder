package com.playgroundfinder.app.data.repository

import com.playgroundfinder.app.BuildConfig
import com.playgroundfinder.app.data.remote.WeatherApiService
import com.playgroundfinder.app.data.remote.dto.WeatherResponse
import com.playgroundfinder.app.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApiService: WeatherApiService
) {
    suspend fun getWeather(lat: Double, lng: Double): Resource<WeatherResponse> {
        return try {
            val response = weatherApiService.getCurrentWeather(
                lat = lat,
                lon = lng,
                apiKey = BuildConfig.WEATHER_API_KEY
            )
            Resource.Success(response)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Időjárás lekérés sikertelen")
        }
    }
}
