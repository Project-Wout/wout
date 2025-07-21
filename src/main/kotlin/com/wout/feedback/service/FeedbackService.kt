package com.wout.feedback.service

import com.wout.common.exception.ApiException
import com.wout.common.exception.ErrorCode.*
import com.wout.feedback.dto.request.FeedbackSubmitRequest
import com.wout.feedback.dto.response.FeedbackHistoryResponse
import com.wout.feedback.dto.response.FeedbackResponse
import com.wout.feedback.dto.response.FeedbackStatisticsResponse
import com.wout.feedback.entity.Feedback
import com.wout.feedback.entity.FeedbackType
import com.wout.feedback.mapper.FeedbackMapper
import com.wout.feedback.repository.FeedbackRepository
import com.wout.member.entity.Member
import com.wout.member.entity.WeatherPreference
import com.wout.member.repository.MemberRepository
import com.wout.member.repository.WeatherPreferenceRepository
import com.wout.member.util.WeatherScoreCalculator
import com.wout.weather.entity.WeatherData
import com.wout.weather.repository.WeatherDataRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.max

/**
 * packageName    : com.wout.feedback.service
 * fileName       : FeedbackService
 * author         : MinKyu Park
 * date           : 2025-06-01
 * description    : 피드백 서비스 (학습률 0.22 & 8개/일 정책 반영)
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-06-01        MinKyu Park       MVP 최초
 * 2025-06-08  MinKyu Park   빠른 학습(3~7일)용 리튠
 *                             • LEARNING_BASE_RATE = 0.22
 *                             • DAILY_LIMIT = 8
 *                             • 신뢰도 0.5 미만 → 미세 학습(0.05)
 *                             • 보정 캡:
 *                                 – 쾌적온도  ±1 ℃
 *                                 – 중요도(COLD/HEAT) ±2 %p
*/
@Service
@Transactional(readOnly = true)
class FeedbackService(
    private val feedbackRepository: FeedbackRepository,
    private val memberRepository: MemberRepository,
    private val weatherPreferenceRepository: WeatherPreferenceRepository,
    private val weatherDataRepository: WeatherDataRepository,
    private val weatherScoreCalculator: WeatherScoreCalculator,
    private val feedbackMapper: FeedbackMapper
) {

    companion object {
        private const val MAX_DAILY_FEEDBACKS    = 8
        private const val STATISTICS_DAYS        = 30
        private const val BASE_LEARNING_RATE     = 0.22       // 신뢰도 가중 기본치
        private const val SMALL_LEARNING_RATE    = 0.05       // 신뢰도 < 0.5 일 때
        private const val TEMP_CAP               = 1          // ±1 ℃
        private const val WEIGHT_CAP             = 0.02       // 2 % (= 0.02)
        private const val MIN_WEIGHT_BOUND       = 0.05       // 5 %
        private const val MAX_WEIGHT_BOUND       = 0.40       // 40 %
    }

    /* ======================== MVP 핵심 기능 ======================== */

    @Transactional
    fun submitFeedback(deviceId: String, request: FeedbackSubmitRequest): FeedbackResponse {
        validateDeviceId(deviceId)
        validateFeedbackRequest(request)

        val member      = findMemberByDeviceId(deviceId)
        val weatherData = findWeatherDataById(request.weatherDataId)
        val preference  = findWeatherPreferenceByMemberId(member.id)

        validateDailyFeedbackLimit(member.id)
        validateDuplicateFeedback(member.id, request.weatherDataId)

        val feedback = createFeedback(member, weatherData, preference, request)
        val saved    = feedbackRepository.save(feedback)
        applyImmediateLearning(preference, saved)

        return feedbackMapper.toResponse(saved)
    }

    fun getFeedbackHistory(deviceId: String, pageable: Pageable): FeedbackHistoryResponse {
        val member = findMemberByDeviceId(deviceId)
        return feedbackMapper.toHistoryResponse(
            feedbackRepository.findByMemberIdOrderByCreatedAtDesc(member.id, pageable)
        )
    }

    fun getFeedbackStatistics(deviceId: String): FeedbackStatisticsResponse {
        val member = findMemberByDeviceId(deviceId)
        val list   = feedbackRepository.findRecentFeedbacks(member.id, STATISTICS_DAYS)
        return feedbackMapper.toStatisticsResponse(list, STATISTICS_DAYS)
    }

    fun canSubmitFeedbackToday(deviceId: String): Map<String, Any> {
        val member   = findMemberByDeviceId(deviceId)
        val todayCnt = getTodayFeedbackCount(member.id)
        return mapOf(
            "canSubmit"            to (todayCnt < MAX_DAILY_FEEDBACKS),
            "remainingCount"       to max(0, MAX_DAILY_FEEDBACKS - todayCnt),
            "maxDailyLimit"        to MAX_DAILY_FEEDBACKS,
            "todaySubmittedCount"  to todayCnt
        )
    }

    /* ======================== Validation ======================== */

    private fun validateDeviceId(id: String) { if (id.isBlank()) throw ApiException(INVALID_INPUT_VALUE) }

    private fun validateFeedbackRequest(r: FeedbackSubmitRequest) =
        try { FeedbackType.fromString(r.feedbackType) } catch (_: IllegalArgumentException) {
            throw ApiException(INVALID_INPUT_VALUE)
        }

    private fun validateDailyFeedbackLimit(memberId: Long) {
        if (getTodayFeedbackCount(memberId) >= MAX_DAILY_FEEDBACKS) throw ApiException(FEEDBACK_LIMIT_EXCEEDED)
    }

    private fun validateDuplicateFeedback(memberId: Long, weatherDataId: Long) {
        if (feedbackRepository.existsByMemberIdAndWeatherDataId(memberId, weatherDataId))
            throw ApiException(DUPLICATE_FEEDBACK)
    }

    private fun getTodayFeedbackCount(memberId: Long): Int {
        val start = LocalDateTime.now().toLocalDate().atStartOfDay()
        val end   = start.plusDays(1).minusNanos(1)
        return feedbackRepository.countTodayFeedbacks(memberId, start, end).toInt()
    }

    private fun findMemberByDeviceId(id: String): Member =
        memberRepository.findByDeviceId(id) ?: throw ApiException(MEMBER_NOT_FOUND)

    private fun findWeatherPreferenceByMemberId(id: Long): WeatherPreference =
        weatherPreferenceRepository.findByMemberId(id) ?: throw ApiException(SENSITIVITY_PROFILE_NOT_FOUND)

    private fun findWeatherDataById(id: Long): WeatherData =
        weatherDataRepository.findById(id).orElseThrow { ApiException(WEATHER_DATA_NOT_FOUND) }

    /* =========================== Core =========================== */

    private fun createFeedback(
        member: Member,
        data: WeatherData,
        pref: WeatherPreference,
        req: FeedbackSubmitRequest
    ): Feedback {
        val feels = pref.calculateFeelsLikeTemperature(data.temperature, data.windSpeed, data.humidity.toDouble())

        val scoreTotal = weatherScoreCalculator.calculateTotalScore(
            data.temperature,
            data.humidity.toDouble(),
            data.uvIndex ?: 0.0,
            data.pm25 ?: 0.0,
            data.pm10 ?: 0.0,
            pref
        ).total.toInt()

        return Feedback.create(
            memberId             = member.id,
            weatherDataId        = req.weatherDataId,
            feedbackType         = FeedbackType.fromString(req.feedbackType),
            actualTemperature    = data.temperature,
            feelsLikeTemperature = feels,
            weatherScore         = scoreTotal,
            previousComfortTemp  = pref.comfortTemperature,
            comments             = req.comments,
            isConfirmed          = req.isConfirmed
        )
    }

    /* ====================== Immediate Learning ====================== */

    @Transactional
    fun applyImmediateLearning(pref: WeatherPreference, fb: Feedback) {
        if (!fb.needsTemperatureAdjustment()) return

        /* 1) 학습률 계산 */
        val reliability  = fb.calculateReliabilityScore()
        val baseRate     = if (reliability < 0.5) SMALL_LEARNING_RATE else BASE_LEARNING_RATE
        val learningRate = reliability * baseRate
        if (learningRate < 0.01) return

        /* 2) ComfortTemperature 조정 (±1 ℃) */
        val tempDeltaRaw = fb.adjustmentAmount * learningRate      // ±2 × r × baseRate
        val tempDelta    = tempDeltaRaw
            .coerceIn(-TEMP_CAP.toDouble(), TEMP_CAP.toDouble())
            .toInt()

        /* 3) Importance(Cold/Heat) 조정 (±2 %) */
        val weightDeltaRaw = WEIGHT_CAP * learningRate             // 최대 0.02
        val weightDelta    = when {
            fb.isColdFeedback() ->  weightDeltaRaw
            fb.isHotFeedback()  -> -weightDeltaRaw
            else                ->  0.0
        }

        val newCold = (pref.importanceCold + weightDelta)
            .coerceIn(MIN_WEIGHT_BOUND, MAX_WEIGHT_BOUND)

        val newHeat = (pref.importanceHeat - weightDelta)
            .coerceIn(MIN_WEIGHT_BOUND, MAX_WEIGHT_BOUND)

        /* 4) 반영 */
        val newComfortTemp = (pref.comfortTemperature + tempDelta).coerceIn(10, 30)

        pref.updatePreferences(
            comfortTemperature = newComfortTemp,
            impCold            = newCold,
            impHeat            = newHeat
        )
    }
}