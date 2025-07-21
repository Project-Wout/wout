package com.wout.member.dto.weather.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank

/**
 * packageName    : com.wout.member.dto.request
 * fileName       : WeatherScoreLocationRequest
 * author         : MinKyu Park
 * date           : 2025-05-27
 * description    : 위치 기반 날씨 점수 요청 DTO
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-27        MinKyu Park       최초 생성
 */
@Schema(description = "위치 기반 날씨 점수 요청")
data class WeatherScoreLocationRequest(

    @field:NotBlank(message = "디바이스 ID는 필수입니다")
    @Schema(description = "사용자 디바이스 ID", example = "device123456")
    val deviceId: String,

    @field:DecimalMin(value = "-90.0", message = "위도는 -90.0 이상이어야 합니다")
    @field:DecimalMax(value = "90.0", message = "위도는 90.0 이하여야 합니다")
    @Schema(description = "위도", example = "35.1595", minimum = "-90.0", maximum = "90.0")
    val latitude: Double,

    @field:DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다")
    @field:DecimalMax(value = "180.0", message = "경도는 180.0 이하여야 합니다")
    @Schema(description = "경도", example = "129.0756", minimum = "-180.0", maximum = "180.0")
    val longitude: Double
)
