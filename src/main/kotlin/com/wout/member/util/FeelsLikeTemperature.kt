package com.wout.member.util

import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sqrt

object FeelsLikeTemperature {

    private const val WIND_CHILL_MAX_TEMP = 10.0
    private const val WIND_CHILL_MIN_WIND_MS = 1.3
    private const val SUMMER_MIN_TEMP = 25.0

    fun apparent(temperature: Double, windSpeedMs: Double, humidity: Double): Double =
        if (temperature >= SUMMER_MIN_TEMP) summer(temperature, humidity)
        else windChill(temperature, windSpeedMs)

    // 기상청 겨울철 체감온도 산출식. 풍속은 km/h, 기온 10℃ 이하 · 풍속 1.3m/s 이상에서만 쓴다.
    fun windChill(temperature: Double, windSpeedMs: Double): Double {
        if (temperature > WIND_CHILL_MAX_TEMP || windSpeedMs < WIND_CHILL_MIN_WIND_MS) return temperature
        val v = (windSpeedMs * 3.6).pow(0.16)
        return 13.12 + 0.6215 * temperature - 11.37 * v + 0.3965 * v * temperature
    }

    // 기상청 여름철 체감온도 산출식, 2022-06-02 개정. 습구온도는 Stull 추정식.
    private fun summer(temperature: Double, humidity: Double): Double {
        val tw = temperature * atan(0.151977 * sqrt(humidity + 8.313659)) +
                atan(temperature + humidity) - atan(humidity - 1.67633) +
                0.00391838 * humidity.pow(1.5) * atan(0.023101 * humidity) - 4.686035
        return -0.2442 + 0.55399 * tw + 0.45535 * temperature -
                0.0022 * tw * tw + 0.00278 * tw * temperature + 3.0
    }
}
