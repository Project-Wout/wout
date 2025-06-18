package com.wout.member.model

/**
 * packageName    : com.wout.member.model
 * fileName       : WeatherGrade
 * author         : MinKyu Park
 * date           : 25. 6. 20.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 6. 20.        MinKyu Park       최초 생성
 */
enum class WeatherGrade(val emoji: String, val description: String) {
    PERFECT ("😊", "완벽한 날씨예요!"),
    GOOD    ("🙂", "대체로 쾌적해요."),
    FAIR    ("😐", "무난한 날씨예요."),
    POOR    ("😕", "조금 불편할 수 있어요, 외출에 주의하세요."),
    TERRIBLE("🥵", "꽤 힘든 날씨예요, 실내 활동을 추천해요.")
}