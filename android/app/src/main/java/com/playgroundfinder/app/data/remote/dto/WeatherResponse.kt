package com.playgroundfinder.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val weather: List<WeatherCondition> = emptyList(),
    val main: WeatherMain,
    val wind: WindInfo,
    val name: String = ""
)

data class WeatherCondition(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class WeatherMain(
    val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    val humidity: Int,
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("temp_max") val tempMax: Double
)

data class WindInfo(
    val speed: Double
)
