package com.wout.member.service

import com.wout.common.exception.ApiException
import com.wout.common.exception.ErrorCode.*
import com.wout.member.dto.response.ElementScoreDetails
import com.wout.member.dto.response.LocationInfo
import com.wout.member.dto.response.WeatherInfo
import com.wout.member.dto.response.WeatherScoreResponse
import com.wout.member.entity.Member
import com.wout.member.entity.WeatherPreference
import com.wout.member.repository.MemberRepository
import com.wout.member.repository.WeatherPreferenceRepository
import com.wout.member.util.WeatherMessage
import com.wout.member.util.WeatherScoreCalculator
import com.wout.member.util.WeatherScoreResult
import com.wout.weather.entity.WeatherData
import com.wout.weather.service.WeatherService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * packageName    : com.wout.member.service
 * fileName       : WeatherScoreService
 * author         : MinKyu Park
 * date           : 2025-05-27
 * description    : 개인화된 날씨 점수 계산 비즈니스 로직
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-27        MinKyu Park       최초 생성
 * 2025-05-29        MinKyu Park       WeatherScoreResponse 생성 로직 공통화
 * 2025-05-31        MinKyu Park       코딩 규칙에 맞게 리팩토링
 * 2025-05-31        MinKyu Park       메시지 처리 로직을 WeatherMessage로 분리
 */
@Service
@Transactional(readOnly = true)
class WeatherScoreService(
    private val weatherScoreCalculator: WeatherScoreCalculator,
    private val weatherMessage: WeatherMessage,
    private val memberRepository: MemberRepository,
    private val weatherPreferenceRepository: WeatherPreferenceRepository,
    private val weatherService: WeatherService
) {

    /**
     * 개인화된 날씨 점수 계산 (위도/경도 기반)
     */
    fun getPersonalizedWeatherScore(
        deviceId: String,
        latitude: Double,
        longitude: Double
    ): WeatherScoreResponse {

        val member = findMemberByDeviceId(deviceId)
        val weatherPreference = findWeatherPreferenceByMemberId(member.id)
        val weatherData = getWeatherData(latitude, longitude)

        return calculateAndBuildResponse(weatherData, weatherPreference, latitude, longitude)
    }

    /**
     * 도시명 기반 날씨 점수 계산
     */
    fun getPersonalizedWeatherScoreByCity(
        deviceId: String,
        cityName: String
    ): WeatherScoreResponse {

        val member = findMemberByDeviceId(deviceId)
        val weatherPreference = findWeatherPreferenceByMemberId(member.id)
        val weatherData = getWeatherDataByCity(cityName)

        return calculateAndBuildResponse(
            weatherData,
            weatherPreference,
            weatherData.latitude,
            weatherData.longitude
        )
    }

    // ===== 공통 조회 메서드들 =====

    private fun findMemberByDeviceId(deviceId: String): Member {
        return memberRepository.findByDeviceId(deviceId) ?: throw ApiException(MEMBER_NOT_FOUND)
    }

    private fun findWeatherPreferenceByMemberId(memberId: Long): WeatherPreference {
        return weatherPreferenceRepository.findByMemberId(memberId)
            ?: throw ApiException(SENSITIVITY_PROFILE_NOT_FOUND)
    }

    private fun getWeatherData(latitude: Double, longitude: Double): WeatherData {
        return try {
            weatherService.getCurrentWeatherData(latitude, longitude)
        } catch (e: Exception) {
            throw ApiException(WEATHER_DATA_NOT_FOUND)
        }
    }

    private fun getWeatherDataByCity(cityName: String): WeatherData {
        return try {
            weatherService.getCurrentWeatherDataByCity(cityName)
        } catch (e: Exception) {
            throw ApiException(WEATHER_DATA_NOT_FOUND)
        }
    }

    // ===== 핵심 비즈니스 로직 =====

    private fun calculateAndBuildResponse(
        weatherData: WeatherData,
        weatherPreference: WeatherPreference,
        latitude: Double,
        longitude: Double
    ): WeatherScoreResponse {
        val scoreResult = calculatePersonalizedScore(weatherData, weatherPreference)
        val personalizedMessage = weatherMessage.generatePersonalizedMessage(scoreResult, weatherPreference)

        return buildWeatherScoreResponse(
            scoreResult = scoreResult,
            personalizedMessage = personalizedMessage,
            weatherData = weatherData,
            weatherPreference = weatherPreference,
            latitude = latitude,
            longitude = longitude
        )
    }

    private fun calculatePersonalizedScore(
        weatherData: WeatherData,
        weatherPreference: WeatherPreference
    ): WeatherScoreResult {
        return try {
            weatherScoreCalculator.calculateTotalScore(
                temperature = weatherData.temperature,
                humidity = weatherData.humidity.toDouble(),
                windSpeed = weatherData.windSpeed,
                uvIndex = weatherData.uvIndex ?: 0.0,
                pm25 = weatherData.pm25 ?: 0.0,
                pm10 = weatherData.pm10 ?: 0.0,
                preference = weatherPreference
            )
        } catch (e: Exception) {
            throw ApiException(INTERNAL_SERVER_ERROR)
        }
    }

    private fun buildWeatherScoreResponse(
        scoreResult: WeatherScoreResult,
        personalizedMessage: String,
        weatherData: WeatherData,
        weatherPreference: WeatherPreference,
        latitude: Double,
        longitude: Double
    ): WeatherScoreResponse {
        return WeatherScoreResponse(
            totalScore = scoreResult.totalScore.toInt(),
            grade = scoreResult.grade,
            message = personalizedMessage,
            elementScores = ElementScoreDetails(
                temperature = scoreResult.elementScores.temperature.toInt(),
                humidity = scoreResult.elementScores.humidity.toInt(),
                wind = scoreResult.elementScores.wind.toInt(),
                uv = scoreResult.elementScores.uv.toInt(),
                airQuality = scoreResult.elementScores.airQuality.toInt()
            ),
            weatherInfo = WeatherInfo(
                temperature = weatherData.temperature,
                feelsLikeTemperature = weatherPreference.calculateFeelsLikeTemperature(
                    weatherData.temperature,
                    weatherData.windSpeed,
                    weatherData.humidity.toDouble()
                ),
                humidity = weatherData.humidity.toDouble(),
                windSpeed = weatherData.windSpeed,
                uvIndex = weatherData.uvIndex ?: 0.0,
                pm25 = weatherData.pm25 ?: 0.0,
                pm10 = weatherData.pm10 ?: 0.0
            ),
            location = LocationInfo(
                latitude = latitude,
                longitude = longitude,
                cityName = weatherData.cityName
            )
        )
    }
}