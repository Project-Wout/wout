package com.wout.member.util

import com.wout.member.entity.WeatherPreference
import com.wout.member.entity.enums.ReactionLevel
import com.wout.member.entity.enums.ReactionLevel.HIGH
import com.wout.member.entity.enums.ReactionLevel.MEDIUM
import com.wout.member.model.WeatherConditions
import com.wout.member.model.WeatherGrade
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeatherMessageTest {

    private val calculator = WeatherScoreCalculator()
    private val weatherMessage = WeatherMessage()

    private val coldWording = "추위를 많이 타시는데"
    private val heatWording = "더위를 많이 타시는데"

    private val heatWave = WeatherConditions(35.0, 0.0, 90.0, 9.0, 10.0, 20.0, 0.0)
    private val windyFreeze = WeatherConditions(0.0, 5.0, 40.0, 1.0, 10.0, 20.0, 0.0)
    private val mildDownpour = WeatherConditions(20.0, 0.0, 90.0, 1.0, 10.0, 20.0, 20.0)
    private val mild = WeatherConditions(21.0, 0.0, 50.0, 3.0, 10.0, 20.0, 0.0)

    private fun preference(rxCold: ReactionLevel = MEDIUM, rxHeat: ReactionLevel = MEDIUM) =
        WeatherPreference.createFromSetup(
            memberId = 1L,
            rxCold = rxCold, rxHeat = rxHeat, rxHumidity = MEDIUM, rxUv = MEDIUM, rxAir = MEDIUM,
            comfortTemperature = 20,
            impCold = 0.2, impHeat = 0.2, impHumidity = 0.2, impUv = 0.2, impAir = 0.2
        )

    private fun message(conditions: WeatherConditions, preference: WeatherPreference): String =
        weatherMessage.generatePersonalizedMessage(calculator.calculateTotalScore(conditions, preference), preference)

    @Test
    fun `추위민감형이라도 폭염으로 점수가 낮은 날에는 추위 문구가 나가지 않는다`() {
        assertFalse(message(heatWave, preference(rxCold = HIGH)).contains(coldWording))
    }

    @Test
    fun `추위민감형은 추위로 점수가 낮은 날에 추위 문구를 받는다`() {
        assertTrue(message(windyFreeze, preference(rxCold = HIGH)).contains(coldWording))
    }

    @Test
    fun `추위와 더위에 모두 민감하면 폭염에는 더위 문구를 받는다`() {
        assertTrue(message(heatWave, preference(rxCold = HIGH, rxHeat = HIGH)).contains(heatWording))
    }

    @Test
    fun `비 때문에만 점수가 낮은 날에는 성향 문구가 나가지 않는다`() {
        assertFalse(message(mildDownpour, preference(rxCold = HIGH)).contains(coldWording))
    }

    @Test
    fun `좋은 날에는 성향 문구가 그대로 나간다`() {
        assertTrue(message(mild, preference(rxCold = HIGH)).contains(coldWording))
    }

    @Test
    fun `문구 구간은 등급과 같은 기준을 쓴다`() {
        val fair = WeatherConditions(21.0, 0.0, 50.0, 3.0, 80.0, 20.0, 0.0)
        val score = calculator.calculateTotalScore(fair, preference())

        assertEquals(WeatherGrade.FAIR, score.grade)
        assertEquals(
            "오늘은 무난한 날씨입니다. 평소처럼 준비하시면 돼요.",
            weatherMessage.generatePersonalizedMessage(score, preference())
        )
    }
}
