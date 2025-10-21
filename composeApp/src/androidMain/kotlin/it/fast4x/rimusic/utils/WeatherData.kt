package it.fast4x.rimusic.utils

data class WeatherData(
    val temp: Double,
    val condition: String,
    val icon: String,
    val humidity: Int,
    val windSpeed: Double,
    val city: String,
    val feelsLike: Double,
    val pressure: Int,
    val visibility: Int,
    val minTemp: Double,
    val maxTemp: Double,
    val sunrise: Long,
    val sunset: Long,
    val timezoneOffset: Int = 0, 
    val cloudCover: Int? = null // 👈 add this field (percentage of clouds)
)
