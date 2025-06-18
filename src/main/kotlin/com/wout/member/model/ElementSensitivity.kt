package com.wout.member.model

import com.wout.member.entity.WeatherPreference
import com.wout.member.entity.enums.ReactionLevel

/**
 * packageName    : com.wout.member.model
 * fileName       : ElementSensitivity
 * author         : MinKyu Park
 * date           : 25. 6. 20.
 * description    : - 각 날씨 요소(추위·더위·습도·UV·공기질)의 민감도 계수를 보관하는 Value Object.
                    - LOW  → 0.5,  MEDIUM → 1.0,  HIGH → 1.5 로 환산해 Calculator 가
                    - ‘편차 × 민감도 계수’ 감점 공식을 적용할 때 사용한다.
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 6. 20.        MinKyu Park       최초 생성
 */
data class ElementSensitivity(
    val cold: Double,
    val heat: Double,
    val humidity: Double,
    val uv: Double,
    val airQuality: Double
) {

    companion object {
        /** Enum → 계수 변환 (LOW 0.5 · MEDIUM 1.0 · HIGH 1.5) */
        private fun toFactor(level: ReactionLevel): Double = when (level) {
            ReactionLevel.LOW    -> 0.5
            ReactionLevel.MEDIUM -> 1.0
            ReactionLevel.HIGH   -> 1.5
        }

        /** 온보딩에서 받은 ReactionLevel 5개로 객체 생성 */
        fun fromReaction(
            cold: ReactionLevel,
            heat: ReactionLevel,
            humi: ReactionLevel,
            uv: ReactionLevel,
            air: ReactionLevel
        ) = ElementSensitivity(
            toFactor(cold),
            toFactor(heat),
            toFactor(humi),
            toFactor(uv),
            toFactor(air)
        )

        /** WeatherPreference 엔티티에서 직접 추출해 생성 */
        fun fromPreference(pref: WeatherPreference) = fromReaction(
            pref.reactionCold,
            pref.reactionHeat,
            pref.reactionHumidity,
            pref.reactionUv,
            pref.reactionAir
        )
    }
}