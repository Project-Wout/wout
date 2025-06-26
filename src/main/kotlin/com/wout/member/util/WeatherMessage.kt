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
     * 1. 점수 기반 한 줄 메시지 (개인 특성 제거됨)
     */
    fun generatePersonalizedMessage(score: WeatherScore, preference: WeatherPreference): String {
        val totalScore = score.total.roundToInt()
        val grade = score.grade

        // ① 기본 part
        val base = "${grade.emoji} $totalScore 점 · ${grade.description}"

        // ② 결론 part
        val conclusion = bucketMessage(totalScore)

        return "$base $conclusion".trim()
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
