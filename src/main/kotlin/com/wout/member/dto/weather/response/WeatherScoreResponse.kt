package com.wout.member.dto.weather.response

import com.wout.member.model.WeatherGrade


/**
 * packageName    : com.wout.member.dto.response
 * fileName       : WeatherScoreResponse
 * author         : MinKyu Park
 * date           : 2025-05-27
 * description    : 날씨 점수 응답 DTO
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-27        MinKyu Park       최초 생성
 */
data class WeatherScoreResponse(
    val totalScore: Int,
    val grade: WeatherGrade,
    val message: String,
    val elementScores: ElementScoreDetailResponse,
    val weatherInfo: WeatherInfo,
    val location: LocationInfo
)

data class WeatherInfo(
    val temperature: Double,
    val feelsLikeTemperature: Double,
    val humidity: Double,
    val windSpeed: Double,
    val uvIndex: Double,
    val pm25: Double,
    val pm10: Double
)

data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val cityName: String
)