package com.wout.member.controller

import com.wout.common.response.ApiResponse
import com.wout.member.dto.weather.request.WeatherScoreCityRequest
import com.wout.member.dto.weather.request.WeatherScoreLocationRequest
import com.wout.member.dto.weather.response.WeatherScoreResponse
import com.wout.member.service.WeatherScoreService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * packageName    : com.wout.member.controller
 * fileName       : WeatherScoreController
 * author         : MinKyu Park
 * date           : 2025-05-27
 * description    : 개인화된 날씨 점수 API 컨트롤러
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-27        MinKyu Park       최초 생성
 */
@Tag(name = "날씨 점수")
@RestController
@RequestMapping("/api/weather-score")
class WeatherScoreController(
    private val weatherScoreService: WeatherScoreService
) {

    @Operation(summary = "위치 기반 날씨 점수 조회")
    @GetMapping("/location")
    fun getWeatherScoreByLocation(
        @Valid request: WeatherScoreLocationRequest
    ): ApiResponse<WeatherScoreResponse> {

        val weatherScore = weatherScoreService.getPersonalizedWeatherScore(
            deviceId = request.deviceId,
            latitude = request.latitude,
            longitude = request.longitude
        )

        return ApiResponse.success(weatherScore)
    }

    @Operation(summary = "도시명 기반 날씨 점수 조회")
    @GetMapping("/city")
    fun getWeatherScoreByCity(
        @Valid request: WeatherScoreCityRequest
    ): ApiResponse<WeatherScoreResponse> {

        val weatherScore = weatherScoreService.getPersonalizedWeatherScoreByCity(
            deviceId = request.deviceId,
            cityName = request.cityName
        )

        return ApiResponse.success(weatherScore)
    }
}