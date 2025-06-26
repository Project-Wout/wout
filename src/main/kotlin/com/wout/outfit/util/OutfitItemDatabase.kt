package com.wout.outfit.util

import com.wout.member.entity.WeatherPreference
import com.wout.outfit.entity.enums.BottomCategory
import com.wout.outfit.entity.enums.OuterCategory
import com.wout.outfit.entity.enums.TopCategory
import com.wout.outfit.entity.enums.WeatherCondition
import com.wout.weather.entity.WeatherData
import org.springframework.stereotype.Component

/**
 * packageName    : com.wout.outfit.util
 * fileName       : OutfitItemDatabase
 * author         : MinKyu Park
 * date           : 2025-06-02
 * description    : 아웃핏 아이템 데이터베이스 (NPE 안전성 개선)
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-06-02        MinKyu Park       최초 생성
 * 2025-06-03        MinKyu Park       OutfitRecommendationEngine 연동 강화
 * 2025-06-03        MinKyu Park       WeatherCondition Enum 적용으로 타입 안전성 확보
 * 2025-06-04        MinKyu Park       NPE 방지를 위한 Safe Call 적용
 */
@Component
class OutfitItemDatabase {

    /* ────────────────── 1. 상·하·외투 ────────────────── */

    fun getTopItems(category: TopCategory, preferences: WeatherPreference): List<String> {
        val base = when (category) {
            TopCategory.SLEEVELESS     -> listOf("민소매", "끈나시", "민소매 티셔츠")
            TopCategory.T_SHIRT        -> listOf("반팔 티셔츠", "면 티셔츠", "폴로 셔츠")
            TopCategory.LINEN_SHIRT    -> listOf("린넨 셔츠", "시어서커 셔츠", "통풍 셔츠")
            TopCategory.LONG_SLEEVE    -> listOf("긴팔 티셔츠", "면 긴팔", "헨리넥")
            TopCategory.LIGHT_SWEATER  -> listOf("얇은 니트", "가벼운 스웨터", "면 가디건")
            TopCategory.SWEATER        -> listOf("니트", "스웨터", "울 니트")
            TopCategory.HOODIE         -> listOf("후드티", "맨투맨", "기모 후드")
            TopCategory.HOODIE_THICK   -> listOf("두꺼운 후드티", "기모 맨투맨", "플리스")
            TopCategory.THICK_SWEATER  -> listOf("두꺼운 니트", "울 스웨터", "목폴라")
        }
        return applyPersonalPreferences(base, category, preferences)
    }

    fun getBottomItems(
        category: BottomCategory, temperature: Double, preferences: WeatherPreference
    ): List<String> {
        val base = when (category) {
            BottomCategory.SHORTS        -> listOf("반바지", "숏 팬츠", "린넨 반바지")
            BottomCategory.LIGHT_PANTS   -> listOf("얇은 면바지", "린넨 바지", "치노 팬츠")
            BottomCategory.JEANS         -> listOf("청바지", "데님 팬츠", "스키니 진")
            BottomCategory.THICK_PANTS   -> listOf("두꺼운 면바지", "코듀로이 팬츠", "울 바지")
            BottomCategory.THERMAL_PANTS -> listOf("기모 바지", "겨울 팬츠", "기모 청바지")
        }
        return applyComfortOptions(base, preferences, temperature)
    }

    fun getOuterItems(category: OuterCategory, temperature: Double): List<String> {
        val base = when (category) {
            OuterCategory.LIGHT_CARDIGAN -> listOf("얇은 가디건", "가벼운 니트")
            OuterCategory.CARDIGAN       -> listOf("가디건", "니트 가디건", "버튼 가디건")
            OuterCategory.LIGHT_JACKET   -> listOf("얇은 자켓", "봄 자켓", "가벼운 점퍼")
            OuterCategory.JACKET         -> listOf("자켓", "재킷", "블레이저")
            OuterCategory.COAT           -> listOf("코트", "트렌치코트", "울코트")
            OuterCategory.PADDING        -> listOf("패딩", "다운 재킷", "겨울 점퍼")
            OuterCategory.WINDBREAKER    -> listOf("바람막이", "윈드브레이커", "레인코트")
        }
        return addWeatherSpecificOptions(base, category, temperature)
    }

    /* ────────────────── 2. 액세서리: 단일 API ────────────────── */

    /**
     * 소품(Accessory) 추천 – WeatherCondition·실측값·민감도 모두 반영
     */
    fun recommendAccessories(
        condition: WeatherCondition,
        weatherData: WeatherData,
        preferences: WeatherPreference
    ): List<String> {
        val acc = mutableListOf<String>()

        /* ① 1차 분기 – WeatherCondition별 기본 세트 */
        when (condition) {
            WeatherCondition.EXTREME_COLD -> {
                acc += listOf("목도리", "장갑", "모자")
                if (weatherData.windSpeed >= 5.0) acc += "방풍 마스크"
            }
            WeatherCondition.COLD_SENSITIVE -> acc += listOf("털모자", "터치장갑", "목도리", "핫팩")
            WeatherCondition.HUMIDITY_RESISTANT -> acc += listOf("메시 모자", "쿨타월")
            WeatherCondition.HEAT_EXTREME -> acc += listOf("쿨토시", "휴대용 선풍기")
            WeatherCondition.PERFECT_WEATHER -> {/* 추가 없음 */}
        }

        /* ② 2차 오버레이 – 자외선 */
        buildUvItems(acc, weatherData.uvIndex ?: 0.0, preferences)

        /* ③ 3차 오버레이 – 미세먼지 */
        buildAirQualityItems(acc, weatherData.pm25 ?: 0.0, preferences)

        /* ④ 4차 오버레이 – 강수·바람 */
        buildRainItems(acc, weatherData.rain1h)
        buildWindItems(acc, weatherData.windSpeed)

        return acc.distinct().take(4)
    }

    /* ── 2-A. 세부 빌더들 ───────────────────────────── */

    private fun buildUvItems(list: MutableList<String>, uv: Double, pref: WeatherPreference) {
        if (uv < 7.0) return
        list += listOf("챙 넓은 모자", "선글라스", "팔토시")
        if (uv >= 9.0 || pref.uvWeightPercent >= 70) {
            list += listOf("UV 차단 장갑", "자외선 차단 스카프")
        }
    }

    private fun buildAirQualityItems(list: MutableList<String>, pm25: Double, pref: WeatherPreference) {
        when {
            pm25 >= 150 -> list += listOf("KF94 마스크", "공기정화 목걸이", "보호안경")
            pm25 >= 75  -> list += "KF94 마스크"
            pm25 >= 35  -> list += "마스크"
        }
        if (pm25 >= 75 && pref.airWeightPercent >= 70) {
            list += "공기정화 목걸이"
        }
    }

    private fun buildRainItems(list: MutableList<String>, rain1h: Double?) {
        val r = rain1h ?: 0.0
        when {
            r >= 10.0 -> list += listOf("장우산", "레인부츠", "우비", "방수 가방")
            r >= 3.0  -> list += listOf("장우산", "방수 신발", "방수 가방")
            r > 0.0   -> list += listOf("우산", "방수 신발")
        }
    }

    private fun buildWindItems(list: MutableList<String>, wind: Double) {
        if (wind >= 7.0) list += listOf("바람막이 모자", "스카프")
    }

    /* ────────────────── 3. 내부 공통 유틸 ────────────────── */

    private fun applyPersonalPreferences(
        base: List<String>, category: TopCategory, pref: WeatherPreference
    ): List<String> {
        val out = base.toMutableList()

        if (pref.isColdSensitive()) when (category) {
            TopCategory.T_SHIRT       -> out += "기모 반팔"
            TopCategory.LONG_SLEEVE   -> out += "기모 긴팔"
            TopCategory.SWEATER       -> out += "터틀넥 니트"
            TopCategory.HOODIE        -> out += "안감 기모 후드"
            else -> {}
        }

        if (pref.isHeatSensitive()) when (category) {
            TopCategory.T_SHIRT       -> out += listOf("메쉬 티셔츠", "쿨링 소재")
            TopCategory.LONG_SLEEVE   -> out += "얇은 긴팔"
            TopCategory.LIGHT_SWEATER -> out += "망사 니트"
            else -> {}
        }

        if (pref.isHumiditySensitive()) out += getHumidityFriendlyOptions(category)

        return out.distinct().take(4)
    }

    private fun applyComfortOptions(
        base: List<String>, pref: WeatherPreference, temp: Double
    ): List<String> {
        val out = base.toMutableList()
        if (pref.isHighSensitivity()) out += listOf("편안한 핏", "스트레치 소재")
        if (pref.isColdSensitive() && temp <= 15) out += listOf("기모 안감", "보온 소재")
        return out.distinct().take(4)
    }

    private fun addWeatherSpecificOptions(
        base: List<String>, category: OuterCategory, temp: Double
    ): List<String> {
        val out = base.toMutableList()
        if (category == OuterCategory.WINDBREAKER) out += listOf("방수 기능", "통기성 소재")
        if (category == OuterCategory.PADDING) {
            when {
                temp <= -10 -> out += listOf("극한기 패딩", "구스다운")
                temp <=   0 -> out += listOf("롱패딩", "방풍 패딩")
                else        -> out += listOf("라이트 패딩", "숏패딩")
            }
        }
        return out.distinct().take(4)
    }

    private fun getHumidityFriendlyOptions(category: TopCategory): List<String> = when (category) {
        TopCategory.T_SHIRT       -> listOf("속건 티셔츠", "통풍 소재")
        TopCategory.LONG_SLEEVE   -> listOf("속건 긴팔", "메쉬 소재")
        TopCategory.LIGHT_SWEATER -> listOf("통풍 니트", "린넨 혼방")
        else -> listOf("통풍 소재")
    }
}