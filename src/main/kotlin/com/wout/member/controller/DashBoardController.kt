package com.wout.member.controller

import com.wout.common.response.ApiResponse
import com.wout.member.dto.dashboard.response.DashboardResponse
import com.wout.member.service.DashboardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * packageName    : com.wout.member.controller
 * fileName       : DashBoardController
 * author         : MinKyu Park
 * date           : 25. 6. 18.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 6. 18.        MinKyu Park       최초 생성
 */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "대시보드 컨트롤러", description = "'홈'탭 관련 API")
class DashBoardController (
    private val dashboardService: DashboardService
){

    @Operation(
        summary = "회원 정보 + 민감도 통합 조회",
        description = "기존 회원의 정보와 날씨 선호도를 함께 조회합니다. (대시보드용)"
    )
    @GetMapping("/{deviceId}")
    fun getMemberWithPreference(
        @Parameter(description = "디바이스 고유 식별자", required = true)
        @PathVariable @NotBlank(message = "Device ID는 필수입니다") deviceId: String
    ): ApiResponse<DashboardResponse> {
        val result = dashboardService.getMemberWithPreference(deviceId)
        return ApiResponse.success(result)
    }
}