package com.wout.member.util

import com.wout.member.entity.WeatherPreference
import com.wout.member.entity.enums.ReactionLevel
import com.wout.member.model.ElementScores
import com.wout.member.model.WeatherGrade
import com.wout.member.model.WeatherScore
import org.springframework.stereotype.Component
import kotlin.math.abs
import kotlin.math.max
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


@Component
class WeatherScoreCalculator {

    /* ---------- (A) 전역·공통 쾌적 구간 ---------- */
    private val comfy = mapOf(
        Elem.COLD to 18.0..24.0,     // 온도: Cold·Heat 공통 구간
        Elem.HEAT to 18.0..24.0,
        Elem.HUMI to 40.0..65.0,     // 습도(%)
        Elem.UV   to 0.0..5.0,       // 자외선 지수
        Elem.AIR  to 0.0..35.0       // 미세먼지(PM10/PM2.5 중 큰 값, ㎍/㎥)
    )

    /* ---------- (B) 극한 구간 고정 감점 ---------- */
    private val extreme = mapOf(
        Elem.COLD to listOf((-1_000.0..5.0) to 15),   // 한파
        Elem.HEAT to listOf(31.0..1_000.0  to 15),    // 폭염
        Elem.HUMI to listOf(85.0..100.0    to 10),
        Elem.UV   to listOf(8.0..20.0      to 10),
        Elem.AIR  to listOf(76.0..1_000.0  to 15)
    )

    /**
     * @param temperature 현재 기온(℃)
     * @param humidity    상대 습도(%)
     * @param uvIndex     자외선 지수
     * @param pm25 / pm10 미세먼지(㎍/㎥) – 둘 중 큰 값을 사용
     * @param preference        사용자 WeatherPreference
     */
    fun calculateTotalScore(
        temperature: Double,
        humidity: Double,
        uvIndex: Double,
        pm25: Double,
        pm10: Double,
        preference: WeatherPreference
    ): WeatherScore {

        /* ① 기본점수 = importance × 100 */
        val base = mapOf(
            Elem.COLD to preference.importanceCold     * 100,
            Elem.HEAT to preference.importanceHeat     * 100,
            Elem.HUMI to preference.importanceHumidity * 100,
            Elem.UV   to preference.importanceUv       * 100,
            Elem.AIR  to preference.importanceAir      * 100
        )

        /* ② 요소별 편차(거리) 계산 */
        val deltas = calcDeltas(
            temperature, humidity, uvIndex, max(pm25, pm10)
        )

        /* ③ 민감도 계수 (HIGH=1.5, MEDIUM=1.0, LOW=0.5) */
        val sensitive = ElementSensitivity.from(preference)

        /* ④ 감점 = delta × 계수 + 극한 패널티 */
        val penalties = mapOf(
            Elem.COLD to deltas[Elem.COLD]!! * sensitive.cold       + extremePenalty(Elem.COLD, temperature),
            Elem.HEAT to deltas[Elem.HEAT]!! * sensitive.heat       + extremePenalty(Elem.HEAT, temperature),
            Elem.HUMI to deltas[Elem.HUMI]!! * sensitive.humidity   + extremePenalty(Elem.HUMI, humidity),
            Elem.UV   to deltas[Elem.UV]!!   * sensitive.uv         + extremePenalty(Elem.UV,   uvIndex),
            Elem.AIR  to deltas[Elem.AIR]!!  * sensitive.airQuality + extremePenalty(Elem.AIR,  max(pm25, pm10))
        )

        /* ⑤ 최종 점수 = max(기본 − 감점, 0) */
        val final = base.mapValues { (k, v) -> (v - penalties[k]!!).coerceAtLeast(0.0) }

        /* ⑥ 총점 & Grade */
        val total = final.values.sum().coerceIn(0.0, 100.0)
        val grade = when (total) {
            in 90.0..100.0 -> WeatherGrade.PERFECT
            in 75.0..89.99 -> WeatherGrade.GOOD
            in 60.0..74.99 -> WeatherGrade.FAIR
            in 45.0..59.99 -> WeatherGrade.POOR
            else           -> WeatherGrade.TERRIBLE
        }

        /* ⑦ DTO 반환 */
        val elemDto = ElementScores(
            cold = final[Elem.COLD]!!.roundToInt(),
            heat = final[Elem.HEAT]!!.roundToInt(),
            humidity = final[Elem.HUMI]!!.roundToInt(),
            uv = final[Elem.UV]!!.roundToInt(),
            airQuality = final[Elem.AIR]!!.roundToInt()
        )

        return WeatherScore(total, grade, elemDto)
    }

    /* ---------- 내부 헬퍼 ---------- */

    /** ‘쾌적 구간’에서 벗어난 거리(절대값) 계산 */
    private fun calcDeltas(
        temp: Double, humi: Double, uv: Double, air: Double
    ): Map<Elem, Double> {

        /* 온도는 Cold / Heat 두 방향으로 분리 */
        val coldDelta = if (temp < comfy[Elem.COLD]!!.start)
            comfy[Elem.COLD]!!.start - temp else 0.0

        val heatDelta = if (temp > comfy[Elem.HEAT]!!.endInclusive)
            temp - comfy[Elem.HEAT]!!.endInclusive else 0.0

        /* 범위 밖 거리 계산(+0 if in range) */
        fun delta(v: Double, range: ClosedFloatingPointRange<Double>) =
            if (v in range) 0.0
            else abs(if (v < range.start) range.start - v else v - range.endInclusive)

        return mapOf(
            Elem.COLD to coldDelta,
            Elem.HEAT to heatDelta,
            Elem.HUMI to delta(humi, comfy[Elem.HUMI]!!),
            Elem.UV   to delta(uv,  comfy[Elem.UV]!!),
            Elem.AIR  to delta(air, comfy[Elem.AIR]!!)
        )
    }

    /** 극한 구간 진입 시 고정 감점, 아니면 0 */
    private fun extremePenalty(e: Elem, value: Double): Int =
        extreme[e]?.firstOrNull { value in it.first }?.second ?: 0

    /* ---------- 내부 전용 타입 ---------- */

    /** 계산용 내부 열거 – 외부에 노출되지 않음 */
    private enum class Elem { COLD, HEAT, HUMI, UV, AIR }

    /** ReactionLevel → 민감도 계수 매핑 */
    private data class ElementSensitivity(
        val cold: Double, val heat: Double, val humidity: Double, val uv: Double, val airQuality: Double
    ) {
        companion object {
            fun from(p: WeatherPreference) = ElementSensitivity(
                cold       = coef(p.reactionCold),
                heat       = coef(p.reactionHeat),
                humidity   = coef(p.reactionHumidity),
                uv         = coef(p.reactionUv),
                airQuality = coef(p.reactionAir)
            )

            private fun coef(r: ReactionLevel): Double = when (r) {
                ReactionLevel.HIGH   -> 1.5
                ReactionLevel.LOW    -> 0.5
                else                 -> 1.0          // MEDIUM
            }
        }
    }
}
