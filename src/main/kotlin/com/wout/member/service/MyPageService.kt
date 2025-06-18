package com.wout.member.service

import com.wout.common.exception.ApiException
import com.wout.common.exception.ErrorCode.MEMBER_NOT_FOUND
import com.wout.common.exception.ErrorCode.SENSITIVITY_PROFILE_NOT_FOUND
import com.wout.member.dto.request.LocationUpdateRequest
import com.wout.member.dto.request.WeatherPreferenceUpdateRequest
import com.wout.member.dto.response.MemberResponse
import com.wout.member.dto.response.WeatherPreferenceResponse
import com.wout.member.entity.Member
import com.wout.member.mapper.MemberMapper
import com.wout.member.mapper.WeatherPreferenceMapper
import com.wout.member.repository.MemberRepository
import com.wout.member.repository.WeatherPreferenceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * packageName    : com.wout.member.service
 * fileName       : MyPageService
 * author         : MinKyu Park
 * date           : 25. 6. 18.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 6. 18.        MinKyu Park       최초 생성
 */
@Service
class MyPageService(
    private val memberRepository: MemberRepository,
    private val weatherPreferenceRepository: WeatherPreferenceRepository,
    private val memberMapper: MemberMapper,
    private val weatherPreferenceMapper: WeatherPreferenceMapper
) {

    /**
     * 회원 정보 조회 (deviceId 기반)
     */
    fun getMemberByDeviceId(deviceId: String): MemberResponse {
        val member = findMemberByDeviceId(deviceId)

        return memberMapper.toResponse(member)
    }

    /**
     * 날씨 선호도 조회
     */
    fun getWeatherPreference(deviceId: String): WeatherPreferenceResponse {
        val member = findMemberByDeviceId(deviceId)
        val weatherPreference =
            weatherPreferenceRepository.findByMemberId(member.id) ?: throw ApiException(SENSITIVITY_PROFILE_NOT_FOUND)

        return weatherPreferenceMapper.toResponse(weatherPreference)
    }

    /**
     *  닉네임 수정
     */
    @Transactional
    fun updateNickname(deviceId: String, nickname: String): MemberResponse {
        val member = findMemberByDeviceId(deviceId)

        member.updateNickname(nickname)

        return memberMapper.toResponse(member)
    }

    /**
     *  기본 위치 정보 수정
     */
    @Transactional
    fun updateLocation(deviceId: String, request: LocationUpdateRequest): MemberResponse {
        val member = findMemberByDeviceId(deviceId)

        member.updateDefaultLocation(
            latitude = request.latitude,
            longitude = request.longitude,
            cityName = request.cityName
        )

        return memberMapper.toResponse(member)
    }

    /**
     * 날씨 선호도 수정
     */
    @Transactional
    fun updateWeatherPreference(
        deviceId: String,
        request: WeatherPreferenceUpdateRequest
    ): WeatherPreferenceResponse {

        request.validate()

        val member = findMemberByDeviceId(deviceId)
        val existingPreference =
            weatherPreferenceRepository.findByMemberId(member.id) ?: throw ApiException(SENSITIVITY_PROFILE_NOT_FOUND)

        existingPreference.updatePreferences(
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

        return weatherPreferenceMapper.toResponse(existingPreference)
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    fun deactivateMember(deviceId: String): MemberResponse {
        val member = findMemberByDeviceId(deviceId)

        member.deactivate()

        return memberMapper.toResponse(member)
    }


    private fun findMemberByDeviceId(deviceId: String): Member {
        return memberRepository.findByDeviceId(deviceId)
            ?: throw ApiException(MEMBER_NOT_FOUND)
    }
}