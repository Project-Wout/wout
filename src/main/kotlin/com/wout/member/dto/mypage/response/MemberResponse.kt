package com.wout.member.dto.mypage.response

import java.time.LocalDateTime

/**
 * packageName    : com.wout.member.dto.response
 * fileName       : MemberResponse
 * author         : MinKyu Park
 * date           : 2025-05-27
 * description    : 회원 정보 응답 DTO
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-27        MinKyu Park       최초 생성
 */
data class MemberResponse(
    val id: Long,
    val deviceId: String,
    val nickname: String?,
    val defaultLatitude: Double?,
    val defaultLongitude: Double?,
    val defaultCityName: String?,
    val hasDefaultLocation: Boolean,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)