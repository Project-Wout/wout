package com.wout.member.mapper

import com.wout.member.dto.request.WeatherPreferenceSetupRequest
import com.wout.member.dto.response.MemberWithPreferenceResponse
import com.wout.member.dto.response.WeatherPreferenceResponse
import com.wout.member.entity.Member
import com.wout.member.entity.WeatherPreference
import com.wout.member.entity.enums.ReactionLevel
import org.springframework.stereotype.Component

/**
 * packageName    : com.wout.member.mapper
 * fileName       : WeatherPreferenceMapper
 * author         : MinKyu Park
 * date           : 2025-05-27
 * description    : WeatherPreference 엔티티와 DTO 간 변환 매퍼
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-27        MinKyu Park       최초 생성
 */
@Component
class WeatherPreferenceMapper(
    private val memberMapper: MemberMapper
) {

    /* ---------- String → ReactionLevel 변환 ---------- */
    private fun toReaction(value: String): ReactionLevel =
        when (value.lowercase()) {
            "high"   -> ReactionLevel.HIGH
            "medium" -> ReactionLevel.MEDIUM
            "low"    -> ReactionLevel.LOW
            else     -> throw IllegalArgumentException("Invalid reaction level: $value")
        }

    /* ---------- SetupRequest → Entity ---------- */
    fun toEntity(memberId: Long, request: WeatherPreferenceSetupRequest): WeatherPreference {
        return WeatherPreference.createFromSetup(
            memberId = memberId,
            rxCold       = toReaction(request.reactionCold),
            rxHeat       = toReaction(request.reactionHeat),
            rxHumidity   = toReaction(request.reactionHumidity),
            rxUv         = toReaction(request.reactionUv),
            rxAir        = toReaction(request.reactionAir),
            comfortTemperature = request.comfortTemperature,
            impCold      = request.importanceCold      / 100.0,
            impHeat      = request.importanceHeat      / 100.0,
            impHumidity  = request.importanceHumidity  / 100.0,
            impUv        = request.importanceUv        / 100.0,
            impAir       = request.importanceAir       / 100.0
        )
    }

    /* ---------- Entity → Response ---------- */
    fun toResponse(preference: WeatherPreference): WeatherPreferenceResponse {
        return WeatherPreferenceResponse(
            id                  = preference.id,
            memberId            = preference.memberId,

            reactionCold        = preference.reactionCold,
            reactionHeat        = preference.reactionHeat,
            reactionHumidity    = preference.reactionHumidity,
            reactionUv          = preference.reactionUv,
            reactionAir         = preference.reactionAir,

            comfortTemperature  = preference.comfortTemperature,

            importanceCold      = (preference.importanceCold      * 100).toInt(),
            importanceHeat      = (preference.importanceHeat      * 100).toInt(),
            importanceHumidity  = (preference.importanceHumidity  * 100).toInt(),
            importanceUv        = (preference.importanceUv        * 100).toInt(),
            importanceAir       = (preference.importanceAir       * 100).toInt(),

            personalTempCorrection = preference.personalTempCorrection,
            createdAt           = preference.createdAt,
            updatedAt           = preference.updatedAt
        )
    }

    /* ---------- Member + Preference → 복합 Response ---------- */
    fun toMemberWithPreferenceResponse(
        member: Member,
        preference: WeatherPreference?
    ): MemberWithPreferenceResponse {
        return MemberWithPreferenceResponse(
            member            = memberMapper.toResponse(member),
            weatherPreference = preference?.let { toResponse(it) }
        )
    }
}