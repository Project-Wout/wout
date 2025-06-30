package com.wout.member.dto.weather.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * packageName    : com.wout.member.dto.request
 * fileName       : WeatherScoreCityRequest
 * author         : MinKyu Park
 * date           : 2025-05-27
 * description    : 도시명 기반 날씨 점수 요청 DTO
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-27        MinKyu Park       최초 생성
 */
@Schema(description = "도시명 기반 날씨 점수 요청")
data class WeatherScoreCityRequest(

    @field:NotBlank(message = "디바이스 ID는 필수입니다")
    @Schema(description = "사용자 디바이스 ID", example = "device123456")
    val deviceId: String,

    @field:NotBlank(message = "도시명은 필수입니다")
    @field:Size(min = 1, max = 50, message = "도시명은 1자 이상 50자 이하여야 합니다")
    @Schema(description = "도시명", example = "부산")
    val cityName: String
)