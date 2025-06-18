package com.wout.member.mapper

import com.wout.member.dto.request.WeatherPreferenceSetupRequest
import com.wout.member.dto.response.MemberWithPreferenceResponse
import com.wout.member.dto.response.WeatherPreferenceResponse
import com.wout.member.entity.Member
import com.wout.member.entity.WeatherPreference
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

    /**
     * SetupRequest -> Entity 변환 (새 설정)
     */
    fun toEntity(memberId: Long, request: WeatherPreferenceSetupRequest): WeatherPreference {
        return WeatherPreference.createFromSetup(
            memberId = memberId,
            priorityFirst = request.priorityFirst,
            prioritySecond = request.prioritySecond,
            comfortTemperature = request.comfortTemperature,
            skinReaction = request.skinReaction,
            humidityReaction = request.humidityReaction,
            temperatureWeight = request.temperatureWeight,
            humidityWeight = request.humidityWeight,
            windWeight = request.windWeight,
            uvWeight = request.uvWeight,
            airQualityWeight = request.airQualityWeight
        )
    }

    /**
     * Entity -> Response 변환
     */
    fun toResponse(preference: WeatherPreference): WeatherPreferenceResponse {
        return WeatherPreferenceResponse(
            id = preference.id,
            memberId = preference.memberId,
            priorityFirst = preference.priorityFirst,
            prioritySecond = preference.prioritySecond,
            priorities = preference.getPriorityList(),
            comfortTemperature = preference.comfortTemperature,
            skinReaction = preference.skinReaction,
            humidityReaction = preference.humidityReaction,
            temperatureWeight = preference.temperatureWeight,
            humidityWeight = preference.humidityWeight,
            windWeight = preference.windWeight,
            uvWeight = preference.uvWeight,
            airQualityWeight = preference.airQualityWeight,
            personalTempCorrection = preference.personalTempCorrection,
            isSetupCompleted = preference.isSetupCompleted,
            createdAt = preference.createdAt,
            updatedAt = preference.updatedAt
        )
    }

    /**
     * Member + WeatherPreference -> MemberWithPreferenceResponse 변환
     */
    fun toMemberWithPreferenceResponse(
        member: Member,
        preference: WeatherPreference?
    ): MemberWithPreferenceResponse {
        return MemberWithPreferenceResponse(
            member = memberMapper.toResponse(member),
            weatherPreference = preference?.let { toResponse(it) }
        )
    }

}