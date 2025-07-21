package com.wout.member.dto.mypage.request

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.hibernate.validator.constraints.Range

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
 * 2025-06-08        MinKyu Park       1-4단계 필드 추가
 */
data class WeatherPreferenceUpdateRequest(

    // ----- ① 민감도(선택) -----
    val reactionCold:     String? = null,
    val reactionHeat:     String? = null,
    val reactionHumidity: String? = null,
    val reactionUv:       String? = null,
    val reactionAir:      String? = null,

    // ----- ② 체감 기준 온도(선택) -----
    @field:Min(10) @field:Max(30)
    val comfortTemperature: Int? = null,

    // ----- ③ 중요도(비중) 1-100 (선택) -----
    @field:Range(min = 1, max = 100)
    val importanceCold:     Int? = null,
    @field:Range(min = 1, max = 100)
    val importanceHeat:     Int? = null,
    @field:Range(min = 1, max = 100)
    val importanceHumidity: Int? = null,
    @field:Range(min = 1, max = 100)
    val importanceUv:       Int? = null,
    @field:Range(min = 1, max = 100)
    val importanceAir:      Int? = null
) {

    /** 중요도 합계(있는 값만) 98~102 검증 */
    @AssertTrue(message = "importance 합계는 98~102 사이여야 합니다")
    fun isImportanceSumValid(): Boolean {
        val list = listOfNotNull(
            importanceCold,
            importanceHeat,
            importanceHumidity,
            importanceUv,
            importanceAir
        )
        if (list.isEmpty()) return true
        val sum = list.sum()
        return sum in 98..102
    }

}