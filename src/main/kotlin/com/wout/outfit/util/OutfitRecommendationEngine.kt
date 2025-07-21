package com.wout.outfit.util

import com.wout.member.entity.WeatherPreference
import com.wout.member.model.WeatherGrade
import com.wout.member.model.WeatherScore
import com.wout.member.util.WeatherScoreCalculator
import com.wout.outfit.entity.OutfitRecommendation
import com.wout.outfit.entity.enums.BottomCategory
import com.wout.outfit.entity.enums.OuterCategory
import com.wout.outfit.entity.enums.TopCategory
import com.wout.outfit.entity.enums.WeatherCondition
import com.wout.weather.entity.WeatherData
import org.springframework.stereotype.Component

/**
 * packageName    : com.wout.outfit.util
 * fileName       : OutfitRecommendationEngine
 * author         : MinKyu Park
 * date           : 2025-06-02
 * description    : 날씨와 사용자 선호도를 기반으로 한 아웃핏 추천 엔진
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-06-02        MinKyu Park       최초 생성
 * 2025-06-03        MinKyu Park       WeatherCondition Enum 적용
 * 2025-06-03        MinKyu Park       NPE 방지, UUID 적용, Reflection 제거
 * 2025-06-03        MinKyu Park       기존 WeatherScoreCalculator 활용
 */
@Component
class OutfitRecommendationEngine(
    private val outfitItemDatabase: OutfitItemDatabase,
    private val weatherScoreCalculator: WeatherScoreCalculator
) {

    /**
     * 메인 추천 함수: 날씨와 사용자 선호도를 기반으로 완전한 아웃핏 추천
     */
    fun generateOutfitRecommendation(
        weatherData: WeatherData,
        preferences: WeatherPreference,
        memberId: Long
    ): OutfitRecommendation {

        val personalFeelsLike = preferences.calculateFeelsLikeTemperature(
            weatherData.temperature,
            weatherData.windSpeed,
            weatherData.humidity.toDouble()
        )

        // WeatherCondition Enum 사용
        val weatherCondition = WeatherCondition.determineCondition(
            personalFeelsLike,
            preferences,
            weatherData
        )

        val topCategory = determineTopCategory(personalFeelsLike, preferences)
        val bottomCategory = determineBottomCategory(personalFeelsLike, preferences)
        val outerCategory = determineOuterCategory(personalFeelsLike, weatherData, preferences)

        // ── 1) 상의 ───────────────────────────────────────────────
        val topRecommendations = outfitItemDatabase.getTopItems(
            category = topCategory,
            preferences = preferences
        )

        // ── 2) 하의 ───────────────────────────────────────────────
        val bottomRecommendations = outfitItemDatabase.getBottomItems(
            category = bottomCategory,
            temperature = personalFeelsLike,   // ← 체감온도
            preferences = preferences
        )

        // ── 3) 외투 (nullable) ───────────────────────────────────
        val outerRecommendations = outerCategory?.let {
            outfitItemDatabase.getOuterItems(
                category = it,
                temperature = personalFeelsLike
            )
        } ?: emptyList()

        // ── 4) 액세서리 ───────────────────────────────────────────
        val accessoryRecommendations = outfitItemDatabase.recommendAccessories(
            condition = weatherCondition,
            weatherData = weatherData,
            preferences = preferences
        )


        // 기존 WeatherScoreCalculator 사용
        val weatherScore = weatherScoreCalculator.calculateTotalScore(
            temperature = weatherData.temperature,
            humidity = weatherData.humidity.toDouble(),
            uvIndex = weatherData.uvIndex ?: 0.0,
            pm25 = weatherData.pm25 ?: 0.0,
            pm10 = weatherData.pm10 ?: 0.0,
            preference = preferences
        )

        // 기존 로직 기반 개인 맞춤 팁 생성
        val personalTip = generatePersonalTipFromScore(weatherScore, preferences)

        return OutfitRecommendation.create(
            memberId = memberId,
            weatherDataId = weatherData.id!!,
            temperature = weatherData.temperature,
            feelsLikeTemperature = personalFeelsLike,
            weatherScore = weatherScore.total.toInt(),
            topCategory = topCategory,
            topItems = topRecommendations,
            bottomCategory = bottomCategory,
            bottomItems = bottomRecommendations,
            outerCategory = outerCategory,
            outerItems = outerRecommendations,
            accessoryItems = accessoryRecommendations,
            recommendationReason = personalTip,
            personalTip = personalTip
        )
    }

    // ===== 카테고리 결정 로직 =====

    /**
     * 상의 카테고리 결정
     */
    private fun determineTopCategory(
        feelsLikeTemperature: Double,
        preferences: WeatherPreference
    ): TopCategory {
        return when {
            feelsLikeTemperature >= 30 -> {
                if (preferences.isHeatSensitive()) TopCategory.SLEEVELESS else TopCategory.T_SHIRT
            }

            feelsLikeTemperature >= 25 -> TopCategory.T_SHIRT
            feelsLikeTemperature >= 22 -> {
                if (preferences.isHumiditySensitive()) TopCategory.LINEN_SHIRT else TopCategory.T_SHIRT
            }

            feelsLikeTemperature >= 20 -> TopCategory.LONG_SLEEVE
            feelsLikeTemperature >= 17 -> {
                if (preferences.isColdSensitive()) TopCategory.SWEATER else TopCategory.LIGHT_SWEATER
            }

            feelsLikeTemperature >= 12 -> TopCategory.SWEATER
            feelsLikeTemperature >= 9 -> {
                if (preferences.isColdSensitive()) TopCategory.THICK_SWEATER else TopCategory.HOODIE
            }

            feelsLikeTemperature >= 5 -> TopCategory.HOODIE_THICK
            else -> TopCategory.THICK_SWEATER
        }
    }

    /**
     * 하의 카테고리 결정
     */
    private fun determineBottomCategory(
        feelsLikeTemperature: Double,
        preferences: WeatherPreference
    ): BottomCategory {
        return when {
            feelsLikeTemperature >= 28 -> BottomCategory.SHORTS
            feelsLikeTemperature >= 20 -> {
                if (preferences.isHumiditySensitive()) BottomCategory.LIGHT_PANTS else BottomCategory.JEANS
            }

            feelsLikeTemperature >= 15 -> BottomCategory.JEANS
            feelsLikeTemperature >= 10 -> {
                if (preferences.isColdSensitive()) BottomCategory.THERMAL_PANTS else BottomCategory.THICK_PANTS
            }

            else -> BottomCategory.THERMAL_PANTS
        }
    }

    /**
     * 외투 카테고리 결정
     */
    private fun determineOuterCategory(
        feelsLikeTemperature: Double,
        weatherData: WeatherData,
        preferences: WeatherPreference
    ): OuterCategory? {
        // Safe call 사용으로 NPE 방지
        val hasRain = weatherData.rain1h?.let { it > 0 } ?: false
        val hasStrongWind = weatherData.windSpeed >= 7.0

        return when {
            feelsLikeTemperature >= 27 -> {
                when {
                    hasRain -> OuterCategory.WINDBREAKER
                    else -> null // 외투 불필요
                }
            }

            feelsLikeTemperature >= 22 -> {
                when {
                    hasRain -> OuterCategory.WINDBREAKER
                    hasStrongWind -> OuterCategory.LIGHT_JACKET
                    preferences.isColdSensitive() -> OuterCategory.LIGHT_CARDIGAN
                    else -> null
                }
            }

            feelsLikeTemperature >= 17 -> {
                when {
                    hasRain -> OuterCategory.WINDBREAKER
                    hasStrongWind -> OuterCategory.JACKET
                    else -> OuterCategory.CARDIGAN
                }
            }

            feelsLikeTemperature >= 12 -> {
                when {
                    hasRain -> OuterCategory.WINDBREAKER
                    hasStrongWind -> OuterCategory.COAT
                    else -> OuterCategory.LIGHT_JACKET
                }
            }

            feelsLikeTemperature >= 5 -> {
                when {
                    hasRain -> OuterCategory.WINDBREAKER
                    hasStrongWind || preferences.isColdSensitive() -> OuterCategory.PADDING
                    else -> OuterCategory.COAT
                }
            }

            else -> OuterCategory.PADDING
        }
    }

    // ===== 개인화 메시지 생성 =====

    /**
     * WeatherCondition Enum 사용한 개인화 메시지 생성
     */
    private fun generatePersonalizedMessage(
        weatherCondition: WeatherCondition,
        feelsLikeTemperature: Double,
        preferences: WeatherPreference,
        weatherData: WeatherData
    ): String {
        val personalityType = determinePersonalityType(preferences)

        return when (weatherCondition) {
            WeatherCondition.EXTREME_COLD -> generateExtremeColdMessage(personalityType, feelsLikeTemperature)
            WeatherCondition.COLD_SENSITIVE -> generateColdSensitiveMessage(personalityType, feelsLikeTemperature)
            WeatherCondition.PERFECT_WEATHER -> generatePerfectWeatherMessage(personalityType)
            WeatherCondition.HUMIDITY_RESISTANT -> generateHumidityMessage(
                personalityType,
                weatherData.humidity.toDouble()
            )

            WeatherCondition.HEAT_EXTREME -> generateHeatExtremeMessage(personalityType, feelsLikeTemperature)
        }
    }

    private fun generateExtremeColdMessage(personalityType: String, temperature: Double): String {
        return when (personalityType) {
            "추위민감형" -> "🥶 ${temperature.toInt()}°C! 평소 추위 많이 타시는데 오늘은 정말 춥네요. 레이어드 착용 필수입니다!"
            "더위민감형" -> "🥶 ${temperature.toInt()}°C. 더위 많이 타시지만 오늘은 두꺼운 옷 꼭 챙기세요!"
            "습도민감형" -> "🥶 ${temperature.toInt()}°C. 습도는 낮지만 기온이 너무 낮아 보온이 최우선입니다!"
            else -> "🥶 ${temperature.toInt()}°C. 매우 추운 날씨입니다. 완전 무장하고 나가세요!"
        }
    }

    private fun generateColdSensitiveMessage(personalityType: String, temperature: Double): String {
        return when (personalityType) {
            "추위민감형" -> "😰 ${temperature.toInt()}°C. 평소 추위 많이 타시는 편이라 한 겹 더 입는 걸 추천해요!"
            "더위민감형" -> "😊 ${temperature.toInt()}°C. 더위 많이 타시는 분께는 적당한 온도예요!"
            else -> "😐 ${temperature.toInt()}°C. 약간 쌀쌀한 날씨입니다."
        }
    }

    private fun generatePerfectWeatherMessage(personalityType: String): String {
        return when (personalityType) {
            "추위민감형" -> "😊 완벽한 날씨네요! 추위 많이 타시는 분도 편안하게 외출하실 수 있어요!"
            "더위민감형" -> "😌 완벽한 날씨입니다! 더위 많이 타시는 분께 딱 맞는 온도예요!"
            "습도민감형" -> "😊 습도도 적당하고 완벽한 날씨입니다!"
            else -> "😊 완벽한 날씨네요! 원하는 스타일로 자유롭게 입으세요!"
        }
    }

    private fun generateHumidityMessage(personalityType: String, humidity: Double): String {
        return when (personalityType) {
            "습도민감형" -> "😰 습도 ${humidity.toInt()}%. 습함을 특히 싫어하시는데 오늘은 통풍 잘 되는 옷 위주로 입으세요!"
            else -> "😐 습도 ${humidity.toInt()}%. 약간 눅눅한 날씨입니다."
        }
    }

    private fun generateHeatExtremeMessage(personalityType: String, temperature: Double): String {
        return when (personalityType) {
            "더위민감형" -> "🔥 ${temperature.toInt()}°C! 더위 많이 타시는데 오늘은 정말 더워요. 시원한 곳 위주로 이동하세요!"
            "추위민감형" -> "🔥 ${temperature.toInt()}°C. 평소 추위 많이 타시지만 오늘은 더위 조심하세요!"
            else -> "🔥 ${temperature.toInt()}°C. 매우 더운 날씨입니다. 시원하게 입고 수분 보충 잊지 마세요!"
        }
    }

    // ===== Helper Methods =====

    private fun determinePersonalityType(preferences: WeatherPreference): String {
        return when {
            preferences.isColdSensitive() -> "추위민감형"
            preferences.isHeatSensitive() -> "더위민감형"
            preferences.isHumiditySensitive() -> "습도민감형"
            else -> "일반형"
        }
    }

    // ===== 개인 맞춤 팁 생성 (기존 점수 기반) =====

    /**
     * WeatherScore 기반 개인 맞춤 팁 생성
     */
    private fun generatePersonalTipFromScore(
        weatherScore: WeatherScore,
        preferences: WeatherPreference
    ): String {

        val tips = mutableListOf<String>()

        /* ─── 1. 등급별 기본 팁 ─── */
        tips += when (weatherScore.grade) {
            WeatherGrade.TERRIBLE -> "🚫 오늘은 날씨가 좋지 않아요. 실내 활동 위주로 쉬어가시는 게 어떨까요?"
            WeatherGrade.POOR -> "⚠️ 다소 불편한 날씨예요. 외출하실 땐 준비를 꼼꼼히 해주세요!"
            WeatherGrade.FAIR -> "🙂 무난한 날씨예요. 가벼운 준비만 해도 충분합니다."
            WeatherGrade.GOOD -> "😄 쾌적한 날씨네요! 편안하게 외출을 즐겨보세요."
            WeatherGrade.PERFECT -> "🌟 완벽한 날씨입니다! 원하는 활동을 마음껏 즐겨보셔도 좋아요."
        }

        /* ─── 2. 민감도 기반 추가 팁 ─── */
        if (preferences.isColdSensitive() && weatherScore.elements.cold < 70) {
            tips += "🥶 추위를 많이 타시는 편이에요. 보온에 신경 써서 레이어드해 보세요!"
        }
        if (preferences.isHeatSensitive() && weatherScore.elements.heat < 70) {
            tips += "🔥 더위를 잘 느끼시는 편이에요. 통풍 잘 되는 시원한 소재를 추천해요!"
        }
        if (preferences.isHumiditySensitive() && weatherScore.elements.humidity < 70) {
            tips += "💧 습도에 민감하시군요. 통풍 좋은 옷감으로 쾌적함을 유지해 보세요!"
        }

        /* ✨ 반드시 String 을 돌려주도록 기본 문구 제공 */
        return tips.firstOrNull() ?: "🙂 오늘 날씨에 맞춰 편안한 차림으로 다녀오세요!"

    }

}