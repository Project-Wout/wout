package com.wout.member.dto.mypage.request

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * packageName    : com.wout.member.dto.request
 * fileName       : LocationUpdateRequest
 * author         : MinKyu Park
 * date           : 2025-05-29
 * description    : 위치 정보 수정 요청 DTO
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-29        MinKyu Park       최초 생성
 */
data class LocationUpdateRequest(
    @field:NotNull(message = "위도는 필수입니다")
    @field:DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
    @field:DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
    val latitude: Double,

    @field:NotNull(message = "경도는 필수입니다")
    @field:DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
    @field:DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
    val longitude: Double,

    @field:Size(max = 100, message = "지역명은 100자를 초과할 수 없습니다")
    val cityName: String? = null
)