package com.wout.member.util

import com.wout.member.entity.WeatherPreference
import com.wout.member.model.WeatherScore
import org.springframework.stereotype.Component
import kotlin.math.roundToInt

/**
 * packageName    : com.wout.member.util
 * fileName       : WeatherMessage
 * author         : MinKyu Park
 * date           : 2025-05-31
 * description    : 사용자에게 표시할 날씨 관련 메시지 (점수 분포·학습 트렌드 리튠)
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-31        MinKyu Park       최초 생성
 * 2025-06-08        MinKyu Park       분포 30~90 중심 & 빠른 학습 메시지 적용
 */
@Component
class WeatherMessage {

    /* ===================== 상수 ===================== */
    companion object {
        /* ---- 개인 특성 키워드 → 메시지 ---- */
        private val PERSONAL_TRAIT_MESSAGES = mapOf(
            "heat" to "더위를 싫어하시는데",
            "cold" to "추위를 많이 타시는 편이라",
            "humidity" to "습함을 특히 싫어하시는데",
            "wind" to "바람에 예민하셔서",
            "uv" to "자외선에 민감하셔서",
            "pollution" to "공기질에 민감하시는데"
        )

        /* ---- 점수대별 결론 ----
         * 새 분포 : 30~90 점 중심 → 경계값 재조정
         */
        private val SCORE_BUCKETS: List<Pair<IntRange, String>> = listOf(
            90..100 to "오늘은 거의 완벽한 날씨예요!",
            75..89 to "전반적으로 쾌적한 날씨예요.",
            55..74 to "무난한 날씨입니다.",
            35..54 to "조금 아쉬운 날씨예요. 옷차림에 신경 써보세요.",
            0..34  to "외출 시 주의가 필요해 보여요."
        )

        /* ---- 학습 트렌드 메시지 ---- */
        private const val HOT_TREND_MSG = "최근 더위를 많이 느끼고 있어요 → 더 시원한 추천으로 학습 중"
        private const val COLD_TREND_MSG = "최근 추위를 많이 느끼고 있어요 → 더 따뜻한 추천으로 학습 중"
        private const val STABLE_MSG = "현재 추천이 안정적으로 맞아가고 있어요!"
    }

    /* ===================== 퍼블릭 API ===================== */

    /**
     * 1. 점수 및 성향 기반 한 줄 메시지 (개인화 강화)
     */
    fun generatePersonalizedMessage(score: WeatherScore, preference: WeatherPreference): String {
        val totalScore = score.total.roundToInt()
        val personality = getPersonalityType(preference)
        val bucket = getScoreBucket(totalScore)

        return when (personality) {
            "추위민감형" -> when (bucket) {
                "PERFECT" -> "추위를 많이 타시는데 오늘은 완벽한 날씨예요! 가볍게 외출해보세요."
                "GOOD"    -> "추위를 많이 타시는데 오늘은 쾌적하게 느껴지실 거예요."
                "FAIR"    -> "추위를 많이 타시는데 오늘은 무난한 날씨입니다. 가벼운 겉옷을 챙겨보세요."
                "POOR"    -> "추위를 많이 타시는데 오늘은 다소 쌀쌀할 수 있어요. 따뜻하게 입으세요."
                "TERRIBLE"-> "추위를 많이 타시는데 오늘은 많이 추울 수 있어요. 보온에 신경 써주세요."
                else      -> "오늘 날씨 정보를 확인할 수 없습니다."
            }
            "더위민감형" -> when (bucket) {
                "PERFECT" -> "더위를 많이 타시는데 오늘은 완벽한 날씨예요! 산뜻하게 외출해보세요."
                "GOOD"    -> "더위를 많이 타시는데 오늘은 쾌적하게 느껴지실 거예요."
                "FAIR"    -> "더위를 많이 타시는데 오늘은 무난한 날씨입니다. 시원하게 입으시면 좋겠어요."
                "POOR"    -> "더위를 많이 타시는데 오늘은 다소 더울 수 있어요. 시원한 옷차림을 추천해요."
                "TERRIBLE"-> "더위를 많이 타시는데 오늘은 많이 더울 수 있어요. 수분 보충 잊지 마세요."
                else      -> "오늘 날씨 정보를 확인할 수 없습니다."
            }
            "습도민감형" -> when (bucket) {
                "PERFECT" -> "습도에 민감하신데 오늘은 완벽한 날씨예요! 쾌적하게 보내세요."
                "GOOD"    -> "습도에 민감하신데 오늘은 쾌적하게 느껴지실 거예요."
                "FAIR"    -> "습도에 민감하신데 오늘은 무난한 날씨입니다. 통풍 잘 되는 옷을 입어보세요."
                "POOR"    -> "습도에 민감하신데 오늘은 다소 습할 수 있어요. 산뜻하게 입으세요."
                "TERRIBLE"-> "습도에 민감하신데 오늘은 많이 습할 수 있어요. 불쾌지수에 주의하세요."
                else      -> "오늘 날씨 정보를 확인할 수 없습니다."
            }
            else -> when (bucket) {
                "PERFECT" -> "오늘은 완벽한 날씨예요! 기분 좋은 하루 보내세요."
                "GOOD"    -> "오늘은 쾌적한 날씨입니다. 외출하기 좋아요."
                "FAIR"    -> "오늘은 무난한 날씨입니다. 평소처럼 준비하시면 돼요."
                "POOR"    -> "오늘은 다소 아쉬운 날씨예요. 옷차림에 신경 써보세요."
                "TERRIBLE"-> "오늘은 외출 시 주의가 필요해 보여요. 건강에 유의하세요."
                else      -> "오늘 날씨 정보를 확인할 수 없습니다."
            }
        }
    }

    // 성향 판별 함수
    private fun getPersonalityType(preference: WeatherPreference): String = when {
        preference.isColdSensitive() -> "추위민감형"
        preference.isHeatSensitive() -> "더위민감형"
        preference.isHumiditySensitive() -> "습도민감형"
        else -> "일반형"
    }

    // 점수 구간 매핑
    private fun getScoreBucket(score: Int): String = when (score) {
        in 90..100 -> "PERFECT"
        in 75..89  -> "GOOD"
        in 55..74  -> "FAIR"
        in 35..54  -> "POOR"
        in 0..34   -> "TERRIBLE"
        else       -> "UNKNOWN"
    }

    /**
     * 2. 최근 학습 방향 메시지 (평균 조정량 전달받음)
     */
    fun generateLearningTrendMessage(avgAdj: Double): String = when {
        avgAdj >= 0.3 -> HOT_TREND_MSG
        avgAdj <= -0.3 -> COLD_TREND_MSG
        else -> STABLE_MSG
    }

    /**
     * 3. 전국 날씨 요약
     */
    fun generateWeatherSummaryMessage(avgT: Double, maxT: Double, minT: Double): String = when {
        avgT >= 25 -> "오늘은 전국적으로 더운 날씨입니다"
        avgT <= 10 -> "오늘은 전국적으로 추운 날씨입니다"
        maxT - minT >= 15 -> "지역별 기온 차가 큰 날씨예요"
        else -> "전반적으로 쾌적한 날씨입니다"
    }

    /* ===================== 헬퍼 ===================== */
    private fun bucketMessage(score: Int): String =
        SCORE_BUCKETS.firstOrNull { score in it.first }?.second ?: "날씨 데이터를 확인할 수 없습니다."

    /* === fallback === */
    fun getDataInsufficientMessage() = "학습 데이터가 부족합니다."
    fun getEmptyWeatherDataMessage() = "현재 날씨 정보를 가져오지 못했습니다."
}
