package com.wout.member.dto.login.request

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * packageName    : com.wout.member.dto.request
 * fileName       : MemberCreateRequest
 * author         : MinKyu Park
 * date           : 2025-05-27
 * description    : 회원 생성 요청 DTO
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-27        MinKyu Park       최초 생성
 */
data class MemberCreateRequest(
    @field:NotBlank(message = "디바이스 ID는 필수입니다")
    @field:Size(max = 255, message = "디바이스 ID는 255자를 초과할 수 없습니다")
    val deviceId: String,

    @field:Size(max = 50, message = "닉네임은 50자를 초과할 수 없습니다")
    val nickname: String? = null,

    @field:DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
    @field:DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
    val latitude: Double? = null,

    @field:DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
    @field:DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
    val longitude: Double? = null,

    @field:Size(max = 100, message = "지역명은 100자를 초과할 수 없습니다")
    val cityName: String? = null
)