package com.wout.member.util

import com.wout.member.entity.WeatherPreference
import com.wout.member.entity.enums.ReactionLevel.MEDIUM
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FeelsLikeTemperatureTest {

    private fun preference(comfortTemperature: Int) = WeatherPreference.createFromSetup(
        memberId = 1L,
        rxCold = MEDIUM, rxHeat = MEDIUM, rxHumidity = MEDIUM, rxUv = MEDIUM, rxAir = MEDIUM,
        comfortTemperature = comfortTemperature,
        impCold = 0.2, impHeat = 0.2, impHumidity = 0.2, impUv = 0.2, impAir = 0.2
    )

    @Test
    fun `겨울 체감온도는 풍속을 km_h로 바꿔 계산한다`() {
        assertEquals(-4.9, FeelsLikeTemperature.windChill(0.0, 5.0), 0.1)
        assertEquals(-4.9, FeelsLikeTemperature.apparent(0.0, 5.0, 40.0), 0.1)
    }

    @Test
    fun `바람이 약하거나 기온이 10도를 넘으면 겨울 식을 쓰지 않는다`() {
        assertEquals(0.0, FeelsLikeTemperature.windChill(0.0, 1.2), 1e-9)
        assertEquals(15.0, FeelsLikeTemperature.apparent(15.0, 8.0, 50.0), 1e-9)
    }

    @Test
    fun `여름 체감온도는 기상청 습구온도 식을 따른다`() {
        assertEquals(32.5, FeelsLikeTemperature.apparent(30.0, 0.0, 85.0), 0.3)
    }

    @Test
    fun `쾌적온도가 높은 사람은 같은 날씨를 더 춥게 느낀다`() {
        assertEquals(17.5, preference(25).calculateFeelsLikeTemperature(20.0, 0.0, 50.0), 1e-9)
        assertEquals(20.0, preference(20).calculateFeelsLikeTemperature(20.0, 0.0, 50.0), 1e-9)
        assertEquals(22.5, preference(15).calculateFeelsLikeTemperature(20.0, 0.0, 50.0), 1e-9)
    }
}
