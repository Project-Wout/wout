package com.wout.member.service

import com.wout.common.exception.ApiException
import com.wout.common.exception.ErrorCode.*
import com.wout.member.dto.request.LocationUpdateRequest
import com.wout.member.dto.request.MemberCreateRequest
import com.wout.member.dto.request.WeatherPreferenceSetupRequest
import com.wout.member.dto.request.WeatherPreferenceUpdateRequest
import com.wout.member.dto.response.MemberResponse
import com.wout.member.dto.response.MemberStatusResponse
import com.wout.member.dto.response.MemberWithPreferenceResponse
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
 * fileName       : MemberService
 * author         : MinKyu Park
 * date           : 2025-06-08
 * description    : 회원 관리 비즈니스 로직 처리 (더티체킹 패턴 적용)
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-27        MinKyu Park       최초 생성
 * 2025-06-01        MinKyu Park       개발 가이드에 맞게 수정
 * 2025-06-08        MinKyu Park       더티체킹 패턴 적용
 */
@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val weatherPreferenceRepository: WeatherPreferenceRepository,
    private val memberMapper: MemberMapper,
    private val weatherPreferenceMapper: WeatherPreferenceMapper
) {

    // ===== 새로운 프로세스: 스플래시용 조회 API =====

    /**
     * 회원 존재 여부 및 설정 완료 상태 확인 (스플래시 전용)
     * 데이터 생성 없이 순수 조회만 수행
     */
    fun checkMemberStatus(deviceId: String): MemberStatusResponse {
        // 1) 입력값 검증
        validateDeviceId(deviceId)

        // 2) 회원 존재 여부 확인
        val memberExists = memberRepository.existsByDeviceId(deviceId)

        // 3) 설정 완료 여부 확인 (회원이 있을 때만)
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

    // ===== 개선된 프로세스: 민감도 설정 시 회원 생성 =====

    /**
     * 민감도 설정과 동시에 회원 생성/업데이트 (원자적 처리)
     * 신규 사용자의 경우 회원 생성 + 민감도 설정을 하나의 트랜잭션으로 처리
     */
    @Transactional
    fun setupWeatherPreferenceWithMember(
        deviceId: String,
        request: WeatherPreferenceSetupRequest
    ): WeatherPreferenceResponse {
        // 1) 입력값 검증
        validateDeviceId(deviceId)
        validateWeatherPreferenceSetupRequest(request)

        // 2) 회원 조회 또는 생성
        val member = memberRepository.findByDeviceId(deviceId)
            ?: createAndSaveMember(MemberCreateRequest(deviceId))

        // 3) 기존 민감도 설정이 있는지 확인
        val existingPreference = weatherPreferenceRepository.findByMemberId(member.id)
        if (existingPreference != null) {
            throw ApiException(WEATHER_PREFERENCE_ALREADY_EXISTS)
        }

        // 4) 민감도 설정 생성 및 저장
        val weatherPreference = weatherPreferenceMapper.toEntity(member.id, request)
        val savedPreference = weatherPreferenceRepository.save(weatherPreference)

        return weatherPreferenceMapper.toResponse(savedPreference)
    }

    // ===== 기존 메서드들 (필요시 유지) =====

    /**
     * 기존 회원 조회 + 민감도 통합 조회 (대시보드용)
     * 이미 가입된 회원의 전체 정보 조회
     */
    fun getMemberWithPreference(deviceId: String): MemberWithPreferenceResponse {
        validateDeviceId(deviceId)

        val member = findMemberByDeviceId(deviceId)
        val weatherPreference = weatherPreferenceRepository.findByMemberId(member.id)

        return weatherPreferenceMapper.toMemberWithPreferenceResponse(member, weatherPreference)
    }

    /**
     * 회원 정보 조회 (deviceId 기반)
     */
    fun getMemberByDeviceId(deviceId: String): MemberResponse {
        validateDeviceId(deviceId)

        val member = findMemberByDeviceId(deviceId)

        return memberMapper.toResponse(member)
    }

    // ===== 수정/업데이트 메서드들 (✅ 더티체킹 패턴 적용) =====

    /**
     * ✅ 닉네임 수정 (더티체킹 적용)
     */
    @Transactional
    fun updateNickname(deviceId: String, nickname: String): MemberResponse {
        validateDeviceId(deviceId)

        val member = findMemberByDeviceId(deviceId)

        member.updateNickname(nickname)

        return memberMapper.toResponse(member)
    }

    /**
     * ✅ 기본 위치 정보 수정 (더티체킹 적용)
     */
    @Transactional
    fun updateLocation(deviceId: String, request: LocationUpdateRequest): MemberResponse {
        validateDeviceId(deviceId)

        val member = findMemberByDeviceId(deviceId)

        member.updateDefaultLocation(
            latitude = request.latitude,
            longitude = request.longitude,
            cityName = request.cityName
        )

        // 4) ✅ save() 불필요 - 트랜잭션 커밋 시 자동 UPDATE
        return memberMapper.toResponse(member)
    }

    /**
     * ✅ 날씨 선호도 수정
     */
    @Transactional
    fun updateWeatherPreference(
        deviceId: String,
        request: WeatherPreferenceUpdateRequest
    ): WeatherPreferenceResponse {
        validateDeviceId(deviceId)

        val member = findMemberByDeviceId(deviceId)
        val existingPreference = findWeatherPreferenceByMemberId(member.id)

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
     * 날씨 선호도 조회
     */
    fun getWeatherPreference(deviceId: String): WeatherPreferenceResponse {
        // 1) 입력값 검증
        validateDeviceId(deviceId)

        // 2) 데이터 조회
        val member = findMemberByDeviceId(deviceId)
        val weatherPreference = findWeatherPreferenceByMemberId(member.id)

        // 3) 응답 생성
        return weatherPreferenceMapper.toResponse(weatherPreference)
    }

    /**
     * ✅ 회원 탈퇴
     */
    @Transactional
    fun deactivateMember(deviceId: String): MemberResponse {
        // 1) 입력값 검증
        validateDeviceId(deviceId)

        // 2) 데이터 조회
        val member = findMemberByDeviceId(deviceId)

        // 3) ✅ 더티체킹: 엔티티 직접 수정
        member.deactivate()

        // 4) ✅ save() 불필요 - 트랜잭션 커밋 시 자동 UPDATE
        return memberMapper.toResponse(member)
    }

    // ===== 기타 비즈니스 메서드들 =====

    /**
     * 5단계 설정 완료 여부 확인
     */
    fun isWeatherPreferenceSetupCompleted(deviceId: String): Boolean {
        if (deviceId.isBlank()) return false

        val member = memberRepository.findByDeviceId(deviceId) ?: return false

        return weatherPreferenceRepository.findByMemberId(member.id)
            ?.isSetupCompleted ?: false
    }

    // ===== 입력값 검증 메서드들 =====

    private fun validateDeviceId(deviceId: String) {
        if (deviceId.isBlank()) {
            throw ApiException(INVALID_INPUT_VALUE)
        }
    }

    private fun validateWeatherPreferenceSetupRequest(request: WeatherPreferenceSetupRequest) {
        if (!request.isValidPriorities()) {
            throw ApiException(INVALID_INPUT_VALUE)
        }
    }

    // ===== 공통 조회 메서드들 =====

    private fun findMemberByDeviceId(deviceId: String): Member {
        return memberRepository.findByDeviceId(deviceId)
            ?: throw ApiException(MEMBER_NOT_FOUND)
    }

    private fun findWeatherPreferenceByMemberId(memberId: Long): com.wout.member.entity.WeatherPreference {
        return weatherPreferenceRepository.findByMemberId(memberId)
            ?: throw ApiException(SENSITIVITY_PROFILE_NOT_FOUND)
    }

    // ===== 비즈니스 로직 메서드들 =====

    private fun createAndSaveMember(request: MemberCreateRequest): Member {
        // Member.from() 팩토리 메서드 사용
        val newMember = Member.from(request)
        return memberRepository.save(newMember)
    }
}