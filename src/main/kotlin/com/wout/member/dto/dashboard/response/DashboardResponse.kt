package com.wout.member.dto.dashboard.response

import com.wout.member.dto.mypage.response.MemberResponse
import com.wout.member.dto.mypage.response.WeatherPreferenceResponse

/**
 * packageName    : com.wout.member.dto.dashboard.response
 * fileName       : DashboardResponse
 * author         : MinKyu Park
 * date           : 25. 6. 30.
 * description    : 
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 6. 30.        MinKyu Park       최초 생성
 */
data class DashboardResponse(
    val member: MemberResponse,
    val weatherPreference: WeatherPreferenceResponse?
)
