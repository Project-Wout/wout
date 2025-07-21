package com.wout.member.model

/**
 * packageName    : com.wout.member.model
 * fileName       : WeatherScore
 * author         : MinKyu Park
 * date           : 25. 6. 20.
 * description    : ▶ 한 번의 계산 결과를 표현하는 도메인 객체.
                    - total    : 요소별 가중평균 총점 (0‒100, Double)
                    - grade    : 총점에 대응되는 등급 enum
                    - elements : 요소별 세부 점수(ElementScores)
                    **계층 사용 범위**
                    • Calculator → Service → (DTO 변환) → Controller
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 6. 20.        MinKyu Park       최초 생성
 */

data class WeatherScore(
    val total: Double,
    val grade: WeatherGrade,
    val elements: ElementScores
)