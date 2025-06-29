package com.wout.member.dto.request

import jakarta.validation.constraints.*
import org.hibernate.validator.constraints.Range

/**
 * packageName    : com.wout.member.dto.request
 * fileName       : WeatherPreferenceSetupRequest
 * author         : MinKyu Park
 * date           : 2025-05-27
 * description    : 5단계 날씨 선호도 설정 요청 DTO
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-27        MinKyu Park       최초 생성
 */
data class WeatherPreferenceSetupRequest(

    // ===== ① 민감도(ReactionLevel) =====
    @field:NotBlank(message = "reactionCold 값은 필수입니다")
    @field:Pattern(
        regexp = "^(high|medium|low)$",
        flags = [Pattern.Flag.CASE_INSENSITIVE],
        message = "reactionCold 값은 high, medium, low 중 하나여야 합니다"
    )
    val reactionCold: String,

    @field:NotBlank(message = "reactionHeat 값은 필수입니다")
    @field:Pattern(
        regexp = "^(high|medium|low)$",
        flags = [Pattern.Flag.CASE_INSENSITIVE],
        message = "reactionHeat 값은 high, medium, low 중 하나여야 합니다"
    )
    val reactionHeat: String,

    @field:NotBlank(message = "reactionHumidity 값은 필수입니다")
    @field:Pattern(
        regexp = "^(high|medium|low)$",
        flags = [Pattern.Flag.CASE_INSENSITIVE],
        message = "reactionHumidity 값은 high, medium, low 중 하나여야 합니다"
    )
    val reactionHumidity: String,

    @field:NotBlank(message = "reactionUv 값은 필수입니다")
    @field:Pattern(
        regexp = "^(high|medium|low)$",
        flags = [Pattern.Flag.CASE_INSENSITIVE],
        message = "reactionUv 값은 high, medium, low 중 하나여야 합니다"
    )
    val reactionUv: String,

    @field:NotBlank(message = "reactionAir 값은 필수입니다")
    @field:Pattern(
        regexp = "^(high|medium|low)$",
        flags = [Pattern.Flag.CASE_INSENSITIVE],
        message = "reactionAir 값은 high, medium, low 중 하나여야 합니다"
    )
    val reactionAir: String,

    // ===== ② 체감 기준 온도 =====
    @field:NotNull(message = "comfortTemperature 값은 필수입니다")
    @field:Min(value = 10, message = "comfortTemperature 는 10℃ 이상이어야 합니다")
    @field:Max(value = 30, message = "comfortTemperature 는 30℃ 이하여야 합니다")
    val comfortTemperature: Int,

    // ===== ③ 요소별 중요도(비중) =====
    @field:Range(min = 1, max = 100, message = "importanceCold 는 1~100 범위여야 합니다")
    val importanceCold: Int,

    @field:Range(min = 1, max = 100, message = "importanceHeat 는 1~100 범위여야 합니다")
    val importanceHeat: Int,

    @field:Range(min = 1, max = 100, message = "importanceHumidity 는 1~100 범위여야 합니다")
    val importanceHumidity: Int,

    @field:Range(min = 1, max = 100, message = "importanceUv 는 1~100 범위여야 합니다")
    val importanceUv: Int,

    @field:Range(min = 1, max = 100, message = "importanceAir 는 1~100 범위여야 합니다")
    val importanceAir: Int
) {

    /** 중요도 합계가 100 ± 2 % 이내인지 검증 */
    @AssertTrue(message = "importance 합계가 98~102 사이여야 합니다")
    fun isValidImportanceSum(): Boolean {
        val sum = importanceCold + importanceHeat + importanceHumidity + importanceUv + importanceAir
        return sum in 98..102
    }
}