package com.wout.outfit.entity.enums

import com.wout.member.entity.WeatherPreference
import com.wout.weather.entity.WeatherData

/**
 * packageName    : com.wout.outfit.entity.enums
 * fileName       : WeatherCondition
 * author         : MinKyu Park
 * date           : 25. 6. 3.
 * description    : 날씨 상황별 조건 enum (아웃핏 추천용) - 비중첩 조건으로 개선
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 6. 3.        MinKyu Park       최초 생성
 * 25. 6. 4.        MinKyu Park       온도 조건 중복 해결, 명확한 우선순위 적용
 */
enum class WeatherCondition(
    val description: String,
    val priority: Int
) {
    EXTREME_COLD("극한 추위", 1),
    COLD_SENSITIVE("추위 민감형", 2),
    HUMIDITY_RESISTANT("습도 민감형", 3),
    HEAT_EXTREME("극한 더위", 4),
    PERFECT_WEATHER("완벽한 날씨", 5);

    companion object {
        /**
         * 온도와 사용자 특성을 기반으로 날씨 조건 결정
         *
         * 우선순위 및 비중첩 조건:
         * 1. 극한 조건 (최우선): 0도 이하, 35도 이상
         * 2. 개인 민감도 조건: 명확한 온도 범위별 분류
         * 3. 습도 조건: 온도 조건과 독립적 평가
         * 4. 기본 조건: 나머지 모든 경우
         *
         * @param temperature 현재 기온
         * @param preferences 사용자 날씨 선호도
         * @param weatherData 날씨 데이터
         * @return 결정된 날씨 조건
         */
        fun determineCondition(
            temperature: Double,
            preferences: WeatherPreference,
            weatherData: WeatherData
        ): WeatherCondition {
            return when {
                // 1단계: 극한 조건 (최우선) - 절대적 임계값
                temperature <= 0 -> EXTREME_COLD
                temperature >= 35 -> HEAT_EXTREME

                // 2단계: 개인 민감도 기반 조건 - 비중첩 명확한 범위
                preferences.isColdSensitive() && temperature in 1.0..15.0 -> COLD_SENSITIVE
                preferences.isHeatSensitive() && temperature in 28.0..34.9 -> HEAT_EXTREME

                // 3단계: 습도 조건 - 온도 조건과 독립적, 적정 온도 구간에서만
                preferences.isHumiditySensitive() &&
                        weatherData.humidity >= 85 &&
                        temperature in 16.0..27.9 -> HUMIDITY_RESISTANT

                // 4단계: 기본 조건 - 나머지 모든 경우
                else -> PERFECT_WEATHER
            }
        }

        /**
         * 조건별 온도 범위 정의 반환 (문서화 및 UI 표시용)
         *
         * @param condition 확인할 날씨 조건
         * @return 해당 조건의 온도 범위 설명
         */
        fun getTemperatureRangeForCondition(condition: WeatherCondition): String {
            return when (condition) {
                EXTREME_COLD -> "0°C 이하"
                COLD_SENSITIVE -> "1°C ~ 15°C (추위 민감형 사용자)"
                HUMIDITY_RESISTANT -> "16°C ~ 27°C (습도 85% 이상, 습도 민감형 사용자)"
                HEAT_EXTREME -> "28°C ~ 34°C (더위 민감형) 또는 35°C 이상"
                PERFECT_WEATHER -> "16°C ~ 27°C (일반 사용자) 또는 기타 조건"
            }
        }

        /**
         * 날씨 조건별 우선순위 정렬
         *
         * @param conditions 정렬할 날씨 조건 리스트
         * @return 우선순위 순으로 정렬된 조건 리스트
         */
        fun sortByPriority(conditions: List<WeatherCondition>): List<WeatherCondition> {
            return conditions.sortedBy { it.priority }
        }

        /**
         * 특정 온도의 기본 조건 반환 (개인 선호도 무시)
         *
         * @param temperature 확인할 기온
         * @return 해당 온도의 기본 날씨 조건
         */
        fun getDefaultConditionForTemperature(temperature: Double): WeatherCondition {
            return when {
                temperature <= 0 -> EXTREME_COLD
                temperature in 1.0..15.0 -> COLD_SENSITIVE
                temperature in 16.0..27.0 -> PERFECT_WEATHER
                temperature in 28.0..34.9 -> HEAT_EXTREME
                temperature >= 35 -> HEAT_EXTREME
                else -> PERFECT_WEATHER  // fallback
            }
        }

        /**
         * 모든 조건의 온도 범위 매핑 반환
         *
         * @return 조건별 온도 범위 맵
         */
        fun getAllTemperatureRanges(): Map<WeatherCondition, String> {
            return WeatherCondition.entries.associateWith { getTemperatureRangeForCondition(it) }
        }


        /**
         * 조건별 추천 아웃핏 힌트 반환
         *
         * @param condition 날씨 조건
         * @return 해당 조건에 적합한 아웃핏 힌트
         */
        fun getOutfitHintForCondition(condition: WeatherCondition): String {
            return when (condition) {
                EXTREME_COLD -> "두꺼운 패딩, 기모 제품, 방한 소품 필수"
                COLD_SENSITIVE -> "레이어드 착용, 얇은 니트 + 가디건 조합"
                HUMIDITY_RESISTANT -> "속건 소재, 통풍 잘 되는 옷, 습기 방지"
                HEAT_EXTREME -> "얇고 시원한 소재, 자외선 차단, 쿨링 아이템"
                PERFECT_WEATHER -> "자유로운 스타일, 취향에 맞는 옷차림"
            }
        }
    }
}