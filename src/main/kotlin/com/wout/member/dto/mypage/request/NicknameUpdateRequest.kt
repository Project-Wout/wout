package com.wout.member.dto.mypage.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * packageName    : com.wout.member.dto.request
 * fileName       : NicknameUpdateRequest
 * author         : MinKyu Park
 * date           : 2025-05-29
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-05-29        MinKyu Park       최초 생성
 */
data class NicknameUpdateRequest(
    @field:NotBlank(message = "닉네임은 필수입니다")
    @field:Size(max = 50, message = "닉네임은 50자를 초과할 수 없습니다")
    val nickname: String
)