package com.wout.member.model

/**
 * packageName    : com.wout.member.model
 * fileName       : ElementScores
 * author         : MinKyu Park
 * date           : 25. 6. 20.
 * description    : ▶ 각 요소(추위·더위·습도·UV·대기질)의
                    “최종 점수(0‒100)”를 담는 값 객체.
                    계산기는 Int 단위(소수점 반올림)로 반환합니다.
                    서비스/컨트롤러 층에서 그대로 DTO 변환에 사용.
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 6. 20.        MinKyu Park       최초 생성
 */
data class ElementScores(
    val cold: Int,
    val heat: Int,
    val humidity: Int,
    val uv: Int,
    val airQuality: Int
)