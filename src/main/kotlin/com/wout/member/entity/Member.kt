package com.wout.member.entity

import com.wout.common.entity.BaseTimeEntity
import jakarta.persistence.*
import org.hibernate.annotations.Comment

/**
 * packageName    : com.wout.member.entity
 * fileName       : Member
 * author         : MinKyu Park
 * date           : 2025-06-01
 * description    : 사용자 기본 정보 엔티티 (언더바 제거, 깔끔한 camelCase 적용)
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-27        MinKyu Park       최초 생성
 * 2025-06-01        MinKyu Park       개발 가이드에 맞게 수정 (Builder 제거, 팩토리 메서드 사용)
 * 2025-06-03        MinKyu Park       언더바 제거, QueryDSL 친화적으로 개선
 */
@Entity
class Member private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("회원 고유 ID")
    val id: Long = 0L,

    @Column(name = "device_id", nullable = false, unique = true, length = 255)
    @Comment("기기 고유 식별자 (익명 사용자 구분용)")
    val deviceId: String,

    @Column(name = "nickname", length = 50)
    @Comment("사용자가 설정한 별명")
    val nickname: String? = null,

    @Column(name = "default_latitude")
    @Comment("기본 위도 (사용자 설정 지역)")
    val defaultLatitude: Double? = null,

    @Column(name = "default_longitude")
    @Comment("기본 경도 (사용자 설정 지역)")
    val defaultLongitude: Double? = null,

    @Column(name = "default_city_name", length = 100)
    @Comment("기본 지역명 (예: 부산 해운대구)")
    val defaultCityName: String? = null,

    @Column(name = "is_active", nullable = false)
    @Comment("활성 상태 (탈퇴 시 false)")
    val isActive: Boolean = true
) : BaseTimeEntity() {

    // JPA용 기본 생성자
    protected constructor() : this(deviceId = "")

    companion object {
        /**
         * 새 회원 생성 (최소 정보)
         */
        fun create(deviceId: String): Member {
            require(deviceId.isNotBlank()) { "DeviceId는 필수값입니다" }
            return Member(deviceId = deviceId)
        }

        /**
         * MemberCreateRequest로부터 생성
         */
        fun from(request: com.wout.member.dto.request.MemberCreateRequest): Member {
            require(request.deviceId.isNotBlank()) { "DeviceId는 필수값입니다" }

            // 위도/경도 유효성 검사
            validateLocation(request.latitude, request.longitude)

            return Member(
                deviceId = request.deviceId,
                nickname = request.nickname,
                defaultLatitude = request.latitude,
                defaultLongitude = request.longitude,
                defaultCityName = request.cityName
            )
        }


        /**
         * 위치 정보 유효성 검증
         */
        private fun validateLocation(latitude: Double?, longitude: Double?) {
            if (latitude != null || longitude != null) {
                require(latitude != null && longitude != null) {
                    "위도와 경도는 함께 설정되어야 합니다"
                }
                require(latitude in -90.0..90.0) { "위도는 -90~90 범위여야 합니다" }
                require(longitude in -180.0..180.0) { "경도는 -180~180 범위여야 합니다" }
            }
        }
    }

    // ===== 도메인 로직 (비즈니스 규칙) =====

    /**
     * 닉네임 변경
     */
    fun updateNickname(newNickname: String?): Member {
        newNickname?.let {
            require(it.length <= 50) { "닉네임은 50자를 초과할 수 없습니다" }
        }
        return copy(nickname = newNickname)
    }

    /**
     * 기본 위치 변경
     */
    fun updateDefaultLocation(
        latitude: Double?,
        longitude: Double?,
        cityName: String?
    ): Member {
        // 위도/경도 유효성 검사
        validateLocation(latitude, longitude)

        return copy(
            defaultLatitude = latitude,
            defaultLongitude = longitude,
            defaultCityName = cityName
        )
    }

    /**
     * 회원 비활성화 (탈퇴)
     */
    fun deactivate(): Member {
        return copy(isActive = false)
    }

    /**
     * 회원 재활성화
     */
    fun reactivate(): Member {
        return copy(isActive = true)
    }

    // ===== 질의 메서드 =====

    /**
     * 기본 위치가 설정되어 있는지 확인
     */
    fun hasDefaultLocation(): Boolean {
        return defaultLatitude != null && defaultLongitude != null
    }

    /**
     * 활성 회원인지 확인
     */
    fun isActiveMember(): Boolean {
        return isActive
    }

    /**
     * 닉네임이 설정되어 있는지 확인
     */
    fun hasNickname(): Boolean {
        return !nickname.isNullOrBlank()
    }

    /**
     * 위치 정보를 Pair로 반환
     */
    fun getLocationPair(): Pair<Double, Double>? {
        return if (hasDefaultLocation()) {
            Pair(defaultLatitude!!, defaultLongitude!!)
        } else null
    }

    /**
     * 회원 기본 정보가 모두 설정되어 있는지 확인
     */
    fun isProfileCompleted(): Boolean {
        return hasNickname() && hasDefaultLocation()
    }

    /**
     * 익명 사용자인지 확인 (닉네임 없음)
     */
    fun isAnonymous(): Boolean {
        return !hasNickname()
    }

    // ===== 불변성 보장을 위한 copy 메서드 =====

    private fun copy(
        nickname: String? = this.nickname,
        defaultLatitude: Double? = this.defaultLatitude,
        defaultLongitude: Double? = this.defaultLongitude,
        defaultCityName: String? = this.defaultCityName,
        isActive: Boolean = this.isActive
    ): Member {
        return Member(
            id = this.id,
            deviceId = this.deviceId,
            nickname = nickname,
            defaultLatitude = defaultLatitude,
            defaultLongitude = defaultLongitude,
            defaultCityName = defaultCityName,
            isActive = isActive
        )
    }
}