package com.wout.member.service

import com.wout.common.exception.ApiException
import com.wout.common.exception.ErrorCode.MEMBER_NOT_FOUND
import com.wout.common.exception.ErrorCode.WEATHER_PREFERENCE_NOT_FOUND
import com.wout.member.dto.response.MemberWithPreferenceResponse
import com.wout.member.mapper.WeatherPreferenceMapper
import com.wout.member.repository.MemberRepository
import com.wout.member.repository.WeatherPreferenceRepository
import org.springframework.stereotype.Service

/**
 * packageName    : com.wout.member.service
 * fileName       : DashboardService
 * author         : MinKyu Park
 * date           : 25. 6. 18.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 6. 18.        MinKyu Park       최초 생성
 */
@Service
class DashboardService (
    private val memberRepository: MemberRepository,
    private val weatherPreferenceRepository: WeatherPreferenceRepository,
    private val weatherPreferenceMapper: WeatherPreferenceMapper
){


    /**
     * 기존 회원 조회 + 민감도 통합 조회 (대시보드용)
     * 이미 가입된 회원의 전체 정보 조회
     */
    fun getMemberWithPreference(deviceId: String): MemberWithPreferenceResponse {
        val member = memberRepository.findByDeviceId(deviceId) ?: throw ApiException(MEMBER_NOT_FOUND)

        val weatherPreference = weatherPreferenceRepository.findByMemberId(member.id) ?:
        throw ApiException(WEATHER_PREFERENCE_NOT_FOUND)


        return weatherPreferenceMapper.toMemberWithPreferenceResponse(member, weatherPreference)
    }

}