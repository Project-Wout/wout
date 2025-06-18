package com.wout.member.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * packageName    : com.wout.member.dto.request
 * fileName       : WeatherPreferenceUpdateRequest
 * author         : MinKyu Park
 * date           : 2025-06-08
 * description    : 날씨 선호도 부분 수정 요청 DTO (확장됨)
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-27        MinKyu Park       최초 생성
 * 2025-06-08        MinKyu Park       1-4단계 필드 추가, 더티체킹 대응
 */
data class WeatherPreferenceUpdateRequest(
    // === 1단계: 우선순위 선택 ===
    val priorityFirst: String? = null,
    val prioritySecond: String? = null,

    // === 2단계: 체감온도 기준점 ===
    @field:Min(value = 10, message = "쾌적 온도는 10도 이상이어야 합니다")
    @field:Max(value = 30, message = "쾌적 온도는 30도 이하여야 합니다")
    val comfortTemperature: Int? = null,

    // === 3-4단계: 피부·습도 반응 ===
    val skinReaction: String? = null,       // "high", "medium", "low"
    val humidityReaction: String? = null,   // "high", "medium", "low"

    // === 5단계: 세부 가중치 (25-75 제한) ===
    @field:Min(value = 25, message = "온도 가중치는 25 이상이어야 합니다")
    @field:Max(value = 75, message = "온도 가중치는 75 이하여야 합니다")
    val temperatureWeight: Int? = null,

    @field:Min(value = 25, message = "습도 가중치는 25 이상이어야 합니다")
    @field:Max(value = 75, message = "습도 가중치는 75 이하여야 합니다")
    val humidityWeight: Int? = null,

    @field:Min(value = 25, message = "바람 가중치는 25 이상이어야 합니다")
    @field:Max(value = 75, message = "바람 가중치는 75 이하여야 합니다")
    val windWeight: Int? = null,

    @field:Min(value = 25, message = "자외선 가중치는 25 이상이어야 합니다")
    @field:Max(value = 75, message = "자외선 가중치는 75 이하여야 합니다")
    val uvWeight: Int? = null,

    @field:Min(value = 25, message = "대기질 가중치는 25 이상이어야 합니다")
    @field:Max(value = 75, message = "대기질 가중치는 75 이하여야 합니다")
    val airQualityWeight: Int? = null
) {

    /**
     * 유효성 검증
     */
    fun validate() {
        // 우선순위 값 검증
        priorityFirst?.let { validatePriorityValue(it, "priorityFirst") }
        prioritySecond?.let { validatePriorityValue(it, "prioritySecond") }

        // 반응 레벨 검증
        skinReaction?.let { validateReactionLevel(it, "skinReaction") }
        humidityReaction?.let { validateReactionLevel(it, "humidityReaction") }

        // 같은 우선순위 중복 방지
        if (priorityFirst != null && prioritySecond != null && priorityFirst == prioritySecond) {
            throw IllegalArgumentException("첫 번째와 두 번째 우선순위는 다르게 설정해야 합니다")
        }
    }

    private fun validatePriorityValue(value: String, fieldName: String) {
        val validPriorities = setOf("heat", "cold", "humidity", "wind", "uv", "pollution")
        if (value !in validPriorities) {
            throw IllegalArgumentException("$fieldName: 유효하지 않은 우선순위 값입니다. 가능한 값: $validPriorities")
        }
    }

    private fun validateReactionLevel(value: String, fieldName: String) {
        val validLevels = setOf("high", "medium", "low")
        if (value !in validLevels) {
            throw IllegalArgumentException("$fieldName: 유효하지 않은 반응 레벨입니다. 가능한 값: $validLevels")
        }
    }
}