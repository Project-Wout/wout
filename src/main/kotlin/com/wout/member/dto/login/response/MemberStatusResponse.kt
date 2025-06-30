package com.wout.member.dto.login.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * packageName    : com.wout.member.dto.response
 * fileName       : MemberStatusResponse
 * author         : MinKyu Park
 * date           : 2025-06-08
 * description    : 스플래시 화면용 회원 상태 확인 응답 DTO
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-06-08        MinKyu Park       최초 생성
 * 2025-06-28        MinKyu Park       단일 필드로 간소화 (회원 존재 = 설정 완료)
 */
@Schema(description = "스플래시 화면용 회원 상태 확인 응답")
data class MemberStatusResponse(

    @Schema(
        description = "설정 완료 여부 (회원 존재 시 true, 신규 사용자 시 false)",
        example = "true",
        required = true
    )
    val isSetupCompleted: Boolean

) {



}