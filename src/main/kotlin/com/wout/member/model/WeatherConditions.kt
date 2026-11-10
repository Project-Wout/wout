package com.wout.member.model

import com.wout.weather.entity.WeatherData

data class WeatherConditions(
    val temperature: Double,
    val windSpeed: Double,
    val humidity: Double,
    val uvIndex: Double?,
    val pm25: Double?,
    val pm10: Double?,
    val precipitation: Double
) {
    companion object {
        fun from(data: WeatherData) = WeatherConditions(
            temperature = data.temperature,
            windSpeed = data.windSpeed,
            humidity = data.humidity.toDouble(),
            uvIndex = data.uvIndex,
            pm25 = data.pm25,
            pm10 = data.pm10,
            precipitation = (data.rain1h ?: 0.0) + (data.snow1h ?: 0.0)
        )
    }
}
