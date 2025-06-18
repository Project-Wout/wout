package com.wout.member.service

import com.wout.common.exception.ApiException
import com.wout.common.exception.ErrorCode.INVALID_INPUT_VALUE
import com.wout.common.exception.ErrorCode.WEATHER_PREFERENCE_ALREADY_EXISTS
import com.wout.member.dto.request.MemberCreateRequest
import com.wout.member.dto.request.WeatherPreferenceSetupRequest
import com.wout.member.dto.response.MemberStatusResponse
import com.wout.member.dto.response.WeatherPreferenceResponse
import com.wout.member.entity.Member
import com.wout.member.mapper.WeatherPreferenceMapper
import com.wout.member.repository.MemberRepository
import com.wout.member.repository.WeatherPreferenceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * packageName    : com.wout.member.service
 * fileName       : LoginService
 * author         : MinKyu Park
 * date           : 25. 6. 18.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 6. 18.        MinKyu Park       최초 생성
 */
@Service
class LoginService(
    private val memberRepository: MemberRepository,
    private val weatherPreferenceRepository: WeatherPreferenceRepository,
    private val weatherPreferenceMapper: WeatherPreferenceMapper
) {
    /**
     * 회원 존재 여부 및 설정 완료 상태 확인 (스플래시 전용)
     * 데이터 생성 없이 순수 조회만 수행
     */
    fun checkMemberStatus(deviceId: String): MemberStatusResponse {
        val memberExists = memberRepository.existsByDeviceId(deviceId)

        // 설정 완료 여부 확인 (회원이 있을 때만)
        val isSetupCompleted = if (memberExists) {
            isWeatherPreferenceSetupCompleted(deviceId)
        } else {
            false
        }

        return MemberStatusResponse(
            memberExists = memberExists,
            isSetupCompleted = isSetupCompleted
        )
    }
    /**
     * 5단계 설정 완료 여부 확인
     */
    fun isWeatherPreferenceSetupCompleted(deviceId: String): Boolean {
        val member = memberRepository.findByDeviceId(deviceId) ?: return false

        return weatherPreferenceRepository.findByMemberId(member.id)
            ?.isSetupCompleted ?: false
    }

    /**
     * 민감도 설정과 동시에 회원 생성/업데이트 (원자적 처리)
     * 신규 사용자의 경우 회원 생성 + 민감도 설정을 하나의 트랜잭션으로 처리
     */
    @Transactional(rollbackFor = [Exception::class])
    fun setupWeatherPreferenceWithMember(
        deviceId: String,
        request: WeatherPreferenceSetupRequest
    ): WeatherPreferenceResponse {
        if (!request.isValidPriorities()) {
            throw ApiException(INVALID_INPUT_VALUE)
        }

        val member = memberRepository.findByDeviceId(deviceId)
            ?: createAndSaveMember(MemberCreateRequest(deviceId))

        // 기존 민감도 설정이 있는지 확인
        val existingPreference = weatherPreferenceRepository.findByMemberId(member.id)
        if (existingPreference != null) {
            throw ApiException(WEATHER_PREFERENCE_ALREADY_EXISTS)
        }

        // 민감도 설정 생성 및 저장
        val weatherPreference = weatherPreferenceMapper.toEntity(member.id, request)
        val savedPreference = weatherPreferenceRepository.save(weatherPreference)

        return weatherPreferenceMapper.toResponse(savedPreference)
    }

    private fun createAndSaveMember(request: MemberCreateRequest): Member {
        val newMember = Member.from(request)
        return memberRepository.save(newMember)
    }

}