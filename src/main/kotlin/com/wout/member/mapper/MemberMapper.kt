package com.wout.member.mapper

import com.wout.member.dto.mypage.response.MemberResponse
import com.wout.member.entity.Member
import org.springframework.stereotype.Component

/**
 * packageName    : com.wout.member.mapper
 * fileName       : MemberMapper
 * author         : MinKyu Park
 * date           : 2025-05-27
 * description    : Member 엔티티와 DTO 간 변환 매퍼
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-27        MinKyu Park       최초 생성
 */
@Component
class MemberMapper {


    fun toResponse(member: Member): MemberResponse {
        return MemberResponse(
            id = member.id,
            deviceId = member.deviceId,
            nickname = member.nickname,
            defaultLatitude = member.defaultLatitude,
            defaultLongitude = member.defaultLongitude,
            defaultCityName = member.defaultCityName,
            hasDefaultLocation = member.hasDefaultLocation(),
            isActive = member.isActive,
            createdAt = member.createdAt,
            updatedAt = member.updatedAt
        )
    }
}