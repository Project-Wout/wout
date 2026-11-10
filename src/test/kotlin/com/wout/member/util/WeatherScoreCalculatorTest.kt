package com.wout.member.util

import com.wout.member.entity.WeatherPreference
import com.wout.member.entity.enums.ReactionLevel
import com.wout.member.entity.enums.ReactionLevel.HIGH
import com.wout.member.entity.enums.ReactionLevel.MEDIUM
import com.wout.member.model.WeatherConditions
import com.wout.member.model.WeatherGrade
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class WeatherScoreCalculatorTest {

    private val calculator = WeatherScoreCalculator()

    private fun preference(
        rxCold: ReactionLevel = MEDIUM,
        comfortTemperature: Int = 20,
        impCold: Double = 0.2, impHeat: Double = 0.2,
        impHumidity: Double = 0.2, impUv: Double = 0.2, impAir: Double = 0.2
    ) = WeatherPreference.createFromSetup(
        memberId = 1L,
        rxCold = rxCold, rxHeat = MEDIUM, rxHumidity = MEDIUM, rxUv = MEDIUM, rxAir = MEDIUM,
        comfortTemperature = comfortTemperature,
        impCold = impCold, impHeat = impHeat, impHumidity = impHumidity, impUv = impUv, impAir = impAir
    )

    private fun conditions(
        temperature: Double,
        humidity: Double = 50.0,
        uvIndex: Double? = 3.0,
        pm25: Double? = 10.0,
        pm10: Double? = 20.0,
        windSpeed: Double = 0.0,
        precipitation: Double = 0.0
    ) = WeatherConditions(temperature, windSpeed, humidity, uvIndex, pm25, pm10, precipitation)

    private fun score(conditions: WeatherConditions, preference: WeatherPreference = preference()) =
        calculator.calculateTotalScore(conditions, preference)

    @Test
    fun `쾌적한 날은 100점이다`() {
        val result = score(conditions(21.0))

        assertEquals(100.0, result.total, 1.0)
        assertEquals(WeatherGrade.PERFECT, result.grade)
    }

    @Test
    fun `폭염에 습하고 자외선이 강한 날은 추위 점수로 구제되지 않는다`() {
        val result = score(conditions(35.0, humidity = 90.0, uvIndex = 9.0))

        assertEquals(12, result.elements.heat)
        assertEquals(0, result.elements.humidity)
        assertEquals(27, result.elements.uv)
        assertEquals(30.0, result.total, 1.0)
        assertEquals(WeatherGrade.TERRIBLE, result.grade)
    }

    @Test
    fun `비가 오면 다른 요소가 좋아도 총점이 강수량 상한에 묶인다`() {
        val rainy = { mm: Double -> score(conditions(20.0, humidity = 90.0, uvIndex = 1.0, precipitation = mm)) }

        assertEquals(100.0, rainy(0.0).total, 1.0)
        assertEquals(85.0, rainy(0.5).total, 1.0)
        assertEquals(70.0, rainy(2.0).total, 1.0)
        assertEquals(55.0, rainy(7.0).total, 1.0)
        assertEquals(45.0, rainy(20.0).total, 1.0)
        assertEquals(WeatherGrade.POOR, rainy(20.0).grade)
    }

    @Test
    fun `바람이 불면 추위 점수는 체감온도로 매긴다`() {
        val result = score(conditions(0.0, humidity = 40.0, uvIndex = 1.0, windSpeed = 5.0))

        assertEquals(8, result.elements.cold)
        assertEquals(38.3, result.total, 1.0)
        assertEquals(WeatherGrade.TERRIBLE, result.grade)
    }

    @Test
    fun `추위 민감도가 높으면 같은 기온에서 추위 감점이 1_5배다`() {
        val chilly = conditions(10.0, uvIndex = 2.0)

        val medium = score(chilly)
        assertEquals(68, medium.elements.cold)
        assertEquals(92.0, medium.total, 1.0)

        val high = score(chilly, preference(rxCold = HIGH))
        assertEquals(52, high.elements.cold)
        assertEquals(82.0, high.total, 1.0)
        assertEquals(WeatherGrade.GOOD, high.grade)
    }

    @Test
    fun `한 요소가 아주 나쁘면 평균에 묻히지 않는다`() {
        val result = score(conditions(21.0, pm25 = 80.0))

        assertEquals(37, result.elements.airQuality)
        assertEquals(67.3, result.total, 1.0)
        assertEquals(WeatherGrade.FAIR, result.grade)
    }

    @Test
    fun `PM10과 PM2_5는 각자 기준으로 채점하고 낮은 쪽을 쓴다`() {
        assertEquals(82, score(conditions(21.0, pm10 = 60.0)).elements.airQuality)
        assertEquals(70, score(conditions(21.0, pm25 = 35.0)).elements.airQuality)
        assertEquals(70, score(conditions(21.0, pm25 = 35.0, pm10 = null)).elements.airQuality)
    }

    @Test
    fun `중요도는 가중치로 쓰인다`() {
        val chilly = conditions(10.0, uvIndex = 2.0)

        val coldMatters = score(
            chilly,
            preference(impCold = 0.4, impHeat = 0.15, impHumidity = 0.15, impUv = 0.15, impAir = 0.15)
        )

        assertEquals(84.9, coldMatters.total, 1.0)
        assertEquals(92.0, score(chilly).total, 1.0)
    }

    @Test
    fun `쾌적온도가 높은 사람은 쾌적 구간이 위로 옮겨진다`() {
        assertEquals(98, score(conditions(20.0), preference(comfortTemperature = 25)).elements.cold)
        assertEquals(100, score(conditions(20.0), preference(comfortTemperature = 20)).elements.cold)

        assertEquals(100, score(conditions(26.0), preference(comfortTemperature = 25)).elements.heat)
        assertEquals(84, score(conditions(26.0), preference(comfortTemperature = 20)).elements.heat)
    }

    @Test
    fun `측정값이 없는 요소는 평균에서 빠진다`() {
        val noUv = score(conditions(21.0, uvIndex = null))
        assertEquals(100.0, noUv.total, 1.0)
        assertEquals(100, noUv.elements.uv)

        val noUvOnBadAirDay = score(conditions(21.0, uvIndex = null, pm25 = 35.0))
        assertEquals(90.0, noUvOnBadAirDay.total, 1.0)

        val noAir = score(conditions(21.0, uvIndex = 6.0, pm25 = null, pm10 = null))
        assertEquals(100, noAir.elements.airQuality)
        assertEquals(90.0, noAir.total, 1.0)
    }

    @Test
    fun `중요도가 측정값 없는 요소에만 있어도 점수가 나온다`() {
        val onlyUvMatters = preference(impCold = 0.0, impHeat = 0.0, impHumidity = 0.0, impUv = 1.0, impAir = 0.0)

        assertEquals(100.0, score(conditions(21.0, uvIndex = null), onlyUvMatters).total, 1.0)
    }

    @Test
    fun `쾌적 구간 끝에서 총점이 끊기지 않는다`() {
        val coldMatters = preference(impCold = 0.4, impHeat = 0.1, impHumidity = 0.1, impUv = 0.2, impAir = 0.2)
        val at = { temperature: Double ->
            score(conditions(temperature, humidity = 90.0, uvIndex = 7.0, pm25 = 55.0), coldMatters).total
        }

        assertTrue(abs(at(24.01) - at(23.99)) < 1.0)
        assertTrue(abs(at(18.01) - at(17.99)) < 1.0)
        assertTrue(at(21.0) - at(24.0) > 3.0)
    }
}
