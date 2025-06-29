package com.wout.member.service

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
 * description    : 로그인 및 초기 회원가입 처리 서비스
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 6. 18.        MinKyu Park       최초 생성
 * 25. 6. 28.        MinKyu Park       불필요한 검증 로직 제거, 플로우 최적화
 */
@Service
class LoginService(
    private val memberRepository: MemberRepository,
    private val weatherPreferenceRepository: WeatherPreferenceRepository,
    private val weatherPreferenceMapper: WeatherPreferenceMapper
) {

    /**
     * 회원 상태 확인 (스플래시 전용)
     * 단일 DB 조회로 최적화 - 회원 존재 여부 = 설정 완료 여부
     */
    fun checkMemberStatus(deviceId: String): MemberStatusResponse {

        val isSetupCompleted = memberRepository.existsByDeviceId(deviceId)

        return MemberStatusResponse(isSetupCompleted = isSetupCompleted)
    }

    /**
     * 민감도 설정과 동시에 회원 생성 (원자적 처리)
     * 신규 사용자 전용 - 온보딩 5단계 완료 시 호출
     *
     * 비즈니스 규칙:
     * - 이 메서드는 신규 사용자만 호출 (checkMemberStatus에서 이미 분기 처리됨)
     * - 따라서 중복 검증 불필요
     */
    @Transactional(rollbackFor = [Exception::class])
    fun setupWeatherPreferenceWithMember(
        deviceId: String,
        request: WeatherPreferenceSetupRequest
    ): WeatherPreferenceResponse {

        val member = createAndSaveMember(MemberCreateRequest(deviceId))

        val weatherPreference = weatherPreferenceMapper.toEntity(member.id, request)

        val savedPreference = weatherPreferenceRepository.save(weatherPreference)

        return weatherPreferenceMapper.toResponse(savedPreference)
    }

    // ===== Private 헬퍼 메서드들 =====

    /**
     * 새 회원 생성 및 저장
     */
    private fun createAndSaveMember(request: MemberCreateRequest): Member {
        val newMember = Member.from(request)
        return memberRepository.save(newMember)
    }


}