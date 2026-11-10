package com.wout.member.util

import com.wout.member.entity.WeatherPreference
import com.wout.member.entity.enums.ReactionLevel
import com.wout.member.model.ElementScores
import com.wout.member.model.WeatherConditions
import com.wout.member.model.WeatherGrade
import com.wout.member.model.WeatherScore
import org.springframework.stereotype.Component
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * packageName    : com.wout.member.util
 * fileName       : WeatherScoreCalculator
 * author         : MinKyu Park
 * date           : 2025-05-27
 * description    : 날씨 점수 계산 전용 유틸리티 클래스
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-27        MinKyu Park       최초 생성
 * 2025-05-29        MinKyu Park       Magic Number 상수화
 * 2025-06-08        MinKyu Park       디버깅 로그 추가
 * 2025-06-08        MinKyu Park       √보정·25~75 가중치 리팩터링
 */

private object ScoreTable {
    const val COMFORT_LOW = 18.0
    const val COMFORT_HIGH = 24.0
    const val COLD_PENALTY_PER_DEGREE = 4.0
    const val HEAT_PENALTY_PER_DEGREE = 8.0

    // 불쾌지수 단계 경계 68 · 75 · 80
    val DISCOMFORT_INDEX = ScoreCurve(68.0 to 100.0, 75.0 to 70.0, 80.0 to 40.0, 85.0 to 0.0)
    const val DRY_HUMIDITY = 30.0
    const val DRY_PENALTY_PER_PERCENT = 1.0

    // 기상청 자외선지수 단계 경계 3 · 6 · 8 · 11
    val UV = ScoreCurve(3.0 to 100.0, 6.0 to 70.0, 8.0 to 40.0, 11.0 to 0.0)

    // 미세먼지 예보 등급 경계: PM10 30 · 80 · 150, PM2.5 15 · 35 · 75
    val PM10 = ScoreCurve(30.0 to 100.0, 80.0 to 70.0, 150.0 to 40.0, 300.0 to 0.0)
    val PM25 = ScoreCurve(15.0 to 100.0, 35.0 to 70.0, 75.0 to 40.0, 150.0 to 0.0)

    const val WORST_ELEMENT_MARGIN = 30.0

    fun precipitationCap(mmLastHour: Double): Double = when {
        mmLastHour >= 15 -> 45.0
        mmLastHour >= 5 -> 55.0
        mmLastHour >= 1 -> 70.0
        mmLastHour > 0 -> 85.0
        else -> 100.0
    }
}

@Component
class WeatherScoreCalculator {

    fun calculateTotalScore(conditions: WeatherConditions, preference: WeatherPreference): WeatherScore {
        fun score(penalty: Double, reaction: ReactionLevel) =
            (100 - penalty * preference.reactionCoefficient(reaction)).coerceIn(0.0, 100.0)

        // 더위 쪽은 실제 기온으로 채점한다. 습도는 습도 요소가 따로 채점하므로 여름 체감온도를 쓰면 두 번 깎인다.
        val temperature = FeelsLikeTemperature.windChill(conditions.temperature, conditions.windSpeed) -
                preference.personalTempCorrection

        val cold = score(
            ScoreTable.COLD_PENALTY_PER_DEGREE * (ScoreTable.COMFORT_LOW - temperature).coerceAtLeast(0.0),
            preference.reactionCold
        )
        val heat = score(
            ScoreTable.HEAT_PENALTY_PER_DEGREE * (temperature - ScoreTable.COMFORT_HIGH).coerceAtLeast(0.0),
            preference.reactionHeat
        )
        val humidity = score(
            100 - ScoreTable.DISCOMFORT_INDEX.scoreAt(discomfortIndex(conditions.temperature, conditions.humidity)) +
                    ScoreTable.DRY_PENALTY_PER_PERCENT * (ScoreTable.DRY_HUMIDITY - conditions.humidity).coerceAtLeast(0.0),
            preference.reactionHumidity
        )
        val uv = conditions.uvIndex?.let { score(100 - ScoreTable.UV.scoreAt(it), preference.reactionUv) }
        val air = listOfNotNull(
            conditions.pm10?.let(ScoreTable.PM10::scoreAt),
            conditions.pm25?.let(ScoreTable.PM25::scoreAt)
        ).minOrNull()?.let { score(100 - it, preference.reactionAir) }

        // 쾌적 구간 안에서 추위 · 더위 중요도를 선형으로 잇는다. 구간 끝에서 가중치가 뛰면 총점이 끊긴다.
        val bandPosition = ((temperature - ScoreTable.COMFORT_LOW) /
                (ScoreTable.COMFORT_HIGH - ScoreTable.COMFORT_LOW)).coerceIn(0.0, 1.0)
        val temperatureWeight = preference.importanceCold +
                (preference.importanceHeat - preference.importanceCold) * bandPosition

        val weighted = listOfNotNull(
            min(cold, heat) to temperatureWeight,
            humidity to preference.importanceHumidity,
            uv?.let { it to preference.importanceUv },
            air?.let { it to preference.importanceAir }
        )
        val weightSum = weighted.sumOf { it.second }
        // 중요도가 측정값 없는 요소에만 몰려 있으면 가중치 합이 0이다.
        val average =
            if (weightSum > 0) weighted.sumOf { it.first * it.second } / weightSum
            else weighted.map { it.first }.average()

        val total = minOf(
            average,
            weighted.minOf { it.first } + ScoreTable.WORST_ELEMENT_MARGIN,
            ScoreTable.precipitationCap(conditions.precipitation)
        )

        return WeatherScore(
            total = total,
            grade = WeatherGrade.fromScore(total),
            elements = ElementScores(
                cold = cold.roundToInt(),
                heat = heat.roundToInt(),
                humidity = humidity.roundToInt(),
                uv = (uv ?: 100.0).roundToInt(),
                airQuality = (air ?: 100.0).roundToInt()
            )
        )
    }

    private fun discomfortIndex(temperature: Double, humidity: Double): Double =
        0.81 * temperature + 0.01 * humidity * (0.99 * temperature - 14.3) + 46.3
}
