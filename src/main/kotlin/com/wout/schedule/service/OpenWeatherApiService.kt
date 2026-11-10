package com.wout.schedule.service

import com.wout.infra.openweather.client.OpenWeatherClient
import com.wout.infra.openweather.dto.request.AirPollutionApiRequest
import com.wout.infra.openweather.dto.request.WeatherApiRequest
import com.wout.schedule.mapper.WeatherApiMapper
import com.wout.weather.entity.WeatherData
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * packageName    : com.wout.schedule.service
 * fileName       : OpenWeatherApiService
 * author         : MinKyu Park
 * date           : 25. 5. 24.
 * description    : OpenWeather API 호출 및 데이터 변환 서비스
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 24.        MinKyu Park       최초 생성
 * 25. 5. 24.        MinKyu Park       UV Index API 추가
 */
@Service
@Transactional
class OpenWeatherApiService(
    private val openWeatherClient: OpenWeatherClient,
    private val weatherApiMapper: WeatherApiMapper,
    @Value("\${openweather.api.key}") private val apiKey: String
) {

    private val log = LoggerFactory.getLogger(OpenWeatherApiService::class.java)

    /**
     * 외부 API 호출을 통한 날씨 데이터 수집
     */
    fun fetchWeatherData(latitude: Double, longitude: Double, cityName: String): WeatherData {
        log.info("🌤️ $cityName 날씨 데이터 수집 시작")
        log.info("📍 좌표: lat=$latitude, lon=$longitude")

        return try {
            // 1. 날씨 정보 API 호출
            val weatherRequest = WeatherApiRequest(
                lat = latitude,
                lon = longitude,
                appid = apiKey,
                units = "metric",
                lang = "kr"
            )

            log.info("📡 날씨 API 요청: $cityName")
            val weatherResponse = openWeatherClient.getCurrentWeather(weatherRequest)
            log.info("✅ 날씨 API 응답 성공: ${weatherResponse.name}")

            // 2. 대기질 정보 API 호출
            val airRequest = AirPollutionApiRequest(
                lat = latitude,
                lon = longitude,
                appid = apiKey
            )

            log.info("🌬️ 대기질 API 요청: $cityName")
            val airResponse = openWeatherClient.getAirPollution(airRequest)
            log.info("✅ 대기질 API 응답 성공: AQI=${airResponse.list.firstOrNull()?.main?.aqi}")

            // 3. UV Index 정보 API 호출
            log.info("☀️ UV Index API 요청: $cityName")
            val uvResponse = try {
                val response = openWeatherClient.getUVIndex(weatherRequest)
                log.info("✅ UV Index API 응답 성공: UV=${response.value}")
                response
            } catch (e: Exception) {
                log.warn("⚠️ $cityName UV Index 조회 실패, 자외선 없이 진행: ${e.message}")
                null
            }

            // 4. API 응답을 WeatherData로 변환
            val weatherData = weatherApiMapper.toWeatherData(
                latitude = latitude,
                longitude = longitude,
                cityName = cityName,
                weatherResponse = weatherResponse,
                airResponse = airResponse,
                uvResponse = uvResponse
            )

            log.info("🎯 $cityName 날씨 데이터 수집 완료!")
            weatherData

        } catch (e: Exception) {
            log.error("❌ $cityName 날씨 데이터 수집 실패", e)
            throw e
        }
    }
}