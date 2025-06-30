package com.wout.member.service

import com.wout.common.exception.ApiException
import com.wout.common.exception.ErrorCode.MEMBER_NOT_FOUND
import com.wout.common.exception.ErrorCode.SENSITIVITY_PROFILE_NOT_FOUND
import com.wout.member.dto.mypage.request.LocationUpdateRequest
import com.wout.member.dto.mypage.request.WeatherPreferenceUpdateRequest
import com.wout.member.dto.mypage.response.MemberResponse
import com.wout.member.dto.mypage.response.WeatherPreferenceResponse
import com.wout.member.entity.Member
import com.wout.member.entity.enums.ReactionLevel
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

        // 1) 회원·기존 선호도 조회
        val member = findMemberByDeviceId(deviceId)
        val preference = weatherPreferenceRepository.findByMemberId(member.id)
            ?: throw ApiException(SENSITIVITY_PROFILE_NOT_FOUND)

        // 2) 문자열 → ReactionLevel 변환 헬퍼
        fun toReaction(str: String?): ReactionLevel? = str?.let {
            when (it.lowercase()) {
                "high"   -> ReactionLevel.HIGH
                "medium" -> ReactionLevel.MEDIUM
                "low"    -> ReactionLevel.LOW
                else     -> throw IllegalArgumentException("Invalid reaction level: $it")
            }
        }

        // 3) 업데이트 (null 값은 유지)
        preference.updatePreferences(
            rxCold        = toReaction(request.reactionCold),
            rxHeat        = toReaction(request.reactionHeat),
            rxHumidity    = toReaction(request.reactionHumidity),
            rxUv          = toReaction(request.reactionUv),
            rxAir         = toReaction(request.reactionAir),
            comfortTemperature = request.comfortTemperature,
            impCold       = request.importanceCold?.div(100.0),
            impHeat       = request.importanceHeat?.div(100.0),
            impHumidity   = request.importanceHumidity?.div(100.0),
            impUv         = request.importanceUv?.div(100.0),
            impAir        = request.importanceAir?.div(100.0)
        )

        // 4) 응답 DTO 변환
        return weatherPreferenceMapper.toResponse(preference)
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