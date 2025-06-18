package com.wout.member.dto.response

/**
 * packageName    : com.wout.member.dto.response
 * fileName       : ElementScoreDetailResponse
 * author         : MinKyu Park
 * date           : 25. 6. 20.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 6. 20.        MinKyu Park       최초 생성
 */
data class ElementScoreDetailResponse(
    val cold: Int,        // 추위 점수
    val heat: Int,        // 더위 점수
    val humidity: Int,    // 습도 점수
    val uv: Int,          // 자외선 점수
    val airQuality: Int   // 대기질(PM) 점수
)