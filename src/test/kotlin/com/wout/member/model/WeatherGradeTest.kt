package com.wout.member.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WeatherGradeTest {

    @Test
    fun `등급 경계는 틈 없이 이상 비교로 나뉜다`() {
        assertEquals(WeatherGrade.PERFECT, WeatherGrade.fromScore(90.0))
        assertEquals(WeatherGrade.GOOD, WeatherGrade.fromScore(89.995))
        assertEquals(WeatherGrade.GOOD, WeatherGrade.fromScore(75.0))
        assertEquals(WeatherGrade.FAIR, WeatherGrade.fromScore(74.999))
        assertEquals(WeatherGrade.FAIR, WeatherGrade.fromScore(60.0))
        assertEquals(WeatherGrade.POOR, WeatherGrade.fromScore(59.999))
        assertEquals(WeatherGrade.POOR, WeatherGrade.fromScore(45.0))
        assertEquals(WeatherGrade.TERRIBLE, WeatherGrade.fromScore(44.999))
    }
}
