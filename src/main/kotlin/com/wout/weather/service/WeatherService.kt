package com.wout.weather.service

import com.wout.common.exception.ApiException
import com.wout.common.exception.ErrorCode.INVALID_INPUT_VALUE
import com.wout.common.exception.ErrorCode.WEATHER_DATA_NOT_FOUND
import com.wout.weather.dto.response.WeatherResponse
import com.wout.weather.dto.response.WeatherSummary
import com.wout.weather.entity.WeatherData
import com.wout.weather.entity.enums.KoreanMajorCity
import com.wout.weather.mapper.WeatherMapper
import com.wout.weather.repository.WeatherDataRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * packageName    : com.wout.weather.service
 * fileName       : WeatherService
 * author         : MinKyu Park
 * date           : 25. 5. 21.
 * description    : 날씨 서비스 (DB 조회 전담)
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 21.        MinKyu Park       최초 생성
 * 25. 5. 24.        MinKyu Park       WeatherMapper 적용, 중복 로직 제거
 * 25. 5. 25.        MinKyu Park       ApiException 통일, WeatherSummary DTO 적용
 * 25. 5. 31.        MinKyu Park       서비스 규칙 준수 리팩토링
 * 2025. 6. 2.       MinKyu Park       검증 메서드 에러 코드 수정 (INVALID_INPUT_VALUE)
 */
@Service
@Transactional(readOnly = true)
class WeatherService(
    private val weatherRepository: WeatherDataRepository,
    private val weatherMapper: WeatherMapper
) {

    companion object {
        private const val WEATHER_CACHE_HOURS = 1L
        private const val WEATHER_HISTORY_HOURS = 24L
        private const val WEATHER_SUMMARY_HOURS = 1L
        private const val ALL_CITIES_CACHE_HOURS = 2L
        private const val HIGH_TEMPERATURE_THRESHOLD = 25.0
        private const val LOW_TEMPERATURE_THRESHOLD = 10.0
        private const val TEMPERATURE_DIFF_THRESHOLD = 15.0
    }

    // ===== 공개 API 메서드들 =====

    /**
     * 사용자 위치 기반 날씨 정보 조회
     */
    fun getWeatherByLocation(userLat: Double, userLon: Double): WeatherResponse {
        validateCoordinates(userLat, userLon)
        val nearestCity = findNearestCity(userLat, userLon)
        val weatherData = findLatestWeatherData(nearestCity.cityName)
        return buildWeatherResponse(weatherData)
    }

    /**
     * 특정 도시의 날씨 정보 조회
     */
    fun getWeatherByCity(cityName: String): WeatherResponse {
        validateCityName(cityName)
        val weatherData = findWeatherDataByCity(cityName)
        return buildWeatherResponse(weatherData)
    }

    /**
     * 모든 주요 도시의 날씨 정보 조회
     */
    fun getAllMajorCitiesWeather(): List<WeatherResponse> {
        val since = LocalDateTime.now().minusHours(ALL_CITIES_CACHE_HOURS)
        return processAllCitiesWeather(since)
    }

    /**
     * 특정 도시의 날씨 히스토리 조회 (최근 24시간)
     */
    fun getWeatherHistory(cityName: String): List<WeatherResponse> {
        validateCityName(cityName)
        val weatherHistoryList = findWeatherHistory(cityName)
        return buildWeatherHistoryResponse(weatherHistoryList)
    }

    /**
     * 현재 날씨 상태 요약 정보 조회
     */
    fun getWeatherSummary(): WeatherSummary {
        val since = LocalDateTime.now().minusHours(WEATHER_SUMMARY_HOURS)
        val allCitiesWeather = weatherRepository.findLatestWeatherForAllCities(since)

        return if (allCitiesWeather.isEmpty()) {
            createEmptyWeatherSummary()
        } else {
            calculateWeatherSummary(allCitiesWeather)
        }
    }

    /**
     * 위도/경도 기반 WeatherData 엔티티 조회 (WeatherScoreService용)
     */
    fun getCurrentWeatherData(userLat: Double, userLon: Double): WeatherData {
        validateCoordinates(userLat, userLon)
        val nearestCity = findNearestCity(userLat, userLon)
        return findLatestWeatherData(nearestCity.cityName)
    }

    /**
     * 도시명 기반 WeatherData 엔티티 조회 (WeatherScoreService용)
     */
    fun getCurrentWeatherDataByCity(cityName: String): WeatherData {
        validateCityName(cityName)
        return findWeatherDataByCity(cityName)
    }

    // ===== 입력값 검증 메서드들 =====

    private fun validateCoordinates(lat: Double, lon: Double) {
        if (lat !in -90.0..90.0) {
            throw ApiException(INVALID_INPUT_VALUE)  // ✅ 수정: WEATHER_DATA_NOT_FOUND → INVALID_INPUT_VALUE
        }
        if (lon !in -180.0..180.0) {
            throw ApiException(INVALID_INPUT_VALUE)  // ✅ 수정: WEATHER_DATA_NOT_FOUND → INVALID_INPUT_VALUE
        }
    }

    private fun validateCityName(cityName: String) {
        if (cityName.isBlank()) {
            throw ApiException(INVALID_INPUT_VALUE)  // ✅ 수정: WEATHER_DATA_NOT_FOUND → INVALID_INPUT_VALUE
        }
    }

    // ===== 공통 조회 메서드들 =====

    private fun findNearestCity(userLat: Double, userLon: Double): KoreanMajorCity {
        return KoreanMajorCity.findNearestCity(userLat, userLon)
    }

    private fun findLatestWeatherData(cityName: String): WeatherData {
        val since = LocalDateTime.now().minusHours(WEATHER_CACHE_HOURS)
        return weatherRepository.findLatestByCityName(cityName, since)
            ?: weatherRepository.findLatestByCityName(cityName)
            ?: throw ApiException(WEATHER_DATA_NOT_FOUND)
    }

    private fun findWeatherDataByCity(cityName: String): WeatherData {
        return weatherRepository.findLatestByCityName(cityName)
            ?: throw ApiException(WEATHER_DATA_NOT_FOUND)
    }

    private fun findWeatherHistory(cityName: String): List<WeatherData> {
        val now = LocalDateTime.now()
        val yesterday = now.minusHours(WEATHER_HISTORY_HOURS)
        return weatherRepository.findByCityNameAndDateRange(cityName, yesterday, now)
    }

    // ===== 핵심 비즈니스 로직 =====

    private fun processAllCitiesWeather(since: LocalDateTime): List<WeatherResponse> {
        return KoreanMajorCity.entries.mapNotNull { city ->
            weatherRepository.findLatestByCityName(city.cityName, since)?.let {
                weatherMapper.toResponseDto(it)
            }
        }
    }

    private fun calculateWeatherSummary(weatherDataList: List<WeatherData>): WeatherSummary {
        val avgTemperature = weatherDataList.map { it.temperature }.average()
        val maxTemperature = weatherDataList.maxOf { it.temperature }
        val minTemperature = weatherDataList.minOf { it.temperature }
        val avgHumidity = weatherDataList.map { it.humidity }.average()
        val avgWindSpeed = weatherDataList.map { it.windSpeed }.average()

        val maxTempCity = weatherDataList.maxByOrNull { it.temperature }?.cityName ?: ""
        val minTempCity = weatherDataList.minByOrNull { it.temperature }?.cityName ?: ""
        val lastUpdated = weatherDataList.maxOf { it.createdAt }

        val message = generateWeatherMessage(avgTemperature, maxTemperature, minTemperature)

        return WeatherSummary(
            availableCities = weatherDataList.size,
            averageTemperature = formatTemperature(avgTemperature),
            maxTemperature = maxTemperature,
            minTemperature = minTemperature,
            averageHumidity = formatHumidity(avgHumidity),
            averageWindSpeed = formatWindSpeed(avgWindSpeed),
            maxTemperatureCity = maxTempCity,
            minTemperatureCity = minTempCity,
            lastUpdated = lastUpdated,
            message = message
        )
    }

    private fun generateWeatherMessage(avgTemp: Double, maxTemp: Double, minTemp: Double): String {
        return when {
            avgTemp >= HIGH_TEMPERATURE_THRESHOLD -> "전국적으로 더운 날씨입니다"
            avgTemp <= LOW_TEMPERATURE_THRESHOLD -> "전국적으로 추운 날씨입니다"
            maxTemp - minTemp >= TEMPERATURE_DIFF_THRESHOLD -> "지역별 기온차가 큰 날씨입니다"
            else -> "전국적으로 쾌적한 날씨입니다"
        }
    }

    // ===== 응답 생성 메서드들 =====

    private fun buildWeatherResponse(weatherData: WeatherData): WeatherResponse {
        return weatherMapper.toResponseDto(weatherData)
    }

    private fun buildWeatherHistoryResponse(weatherDataList: List<WeatherData>): List<WeatherResponse> {
        return weatherDataList.map { weatherMapper.toResponseDto(it) }
    }

    private fun createEmptyWeatherSummary(): WeatherSummary {
        return WeatherSummary(
            availableCities = 0,
            averageTemperature = 0.0,
            maxTemperature = 0.0,
            minTemperature = 0.0,
            averageHumidity = 0.0,
            averageWindSpeed = 0.0,
            maxTemperatureCity = "",
            minTemperatureCity = "",
            lastUpdated = LocalDateTime.now(),
            message = "현재 날씨 데이터를 조회할 수 없습니다."
        )
    }

    private fun formatTemperature(temperature: Double): Double {
        return String.format(Locale.US, "%.1f", temperature).toDouble()  // ✅ Locale 명시로 환경 독립성 보장
    }

    private fun formatHumidity(humidity: Double): Double {
        return String.format(Locale.US, "%.1f", humidity).toDouble()  // ✅ Locale 명시로 환경 독립성 보장
    }

    private fun formatWindSpeed(windSpeed: Double): Double {
        return String.format(Locale.US, "%.1f", windSpeed).toDouble()  // ✅ Locale 명시로 환경 독립성 보장
    }
}