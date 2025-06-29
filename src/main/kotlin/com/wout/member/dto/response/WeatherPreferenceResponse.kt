package com.wout.member.dto.response

import com.wout.member.entity.enums.ReactionLevel
import java.time.LocalDateTime

/**
 * packageName    : com.wout.member.dto.response
 * fileName       : WeatherPreferenceResponse
 * author         : MinKyu Park
 * date           : 2025-05-27
 * description    : 날씨 선호도 응답 DTO
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-27        MinKyu Park       최초 생성
 */
data class WeatherPreferenceResponse(
    val id: Long,
    val memberId: Long,

    // ===== ① 민감도(ReactionLevel) =====
    val reactionCold: ReactionLevel,
    val reactionHeat: ReactionLevel,
    val reactionHumidity: ReactionLevel,
    val reactionUv: ReactionLevel,
    val reactionAir: ReactionLevel,

    // ===== ② 체감 기준 온도 =====
    val comfortTemperature: Int,

    // ===== ③ 요소별 중요도(비중) (1-100) =====
    val importanceCold: Int,
    val importanceHeat: Int,
    val importanceHumidity: Int,
    val importanceUv: Int,
    val importanceAir: Int,

    // ===== 파생값 =====
    val personalTempCorrection: Double,

    // ===== 메타 =====
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)