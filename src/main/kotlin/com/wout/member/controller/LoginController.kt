package com.wout.member.controller

import com.wout.common.response.ApiResponse
import com.wout.member.dto.request.WeatherPreferenceSetupRequest
import com.wout.member.dto.response.MemberStatusResponse
import com.wout.member.dto.response.WeatherPreferenceResponse
import com.wout.member.service.LoginService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.*

/**
 * packageName    : com.wout.member.controller
 * fileName       : LoginController
 * author         : MinKyu Park
 * date           : 25. 6. 18.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 6. 18.        MinKyu Park       최초 생성
 */
@RestController
@RequestMapping("/api/login")
@Tag(name = "로그인", description = "스플래시 ~ 온보딩 ~ 메인페이지 과정까지 관리하는 컨트롤러")
class LoginController (
    private val loginService: LoginService,
){

    @Operation(
        summary = "회원 상태 확인",
        description = "회원 존재 여부 및 설정 완료 상태를 확인합니다. (스플래시 화면용)"
    )
    @GetMapping("/status/{deviceId}")
    fun checkMemberStatus(
        @Parameter(description = "디바이스 고유 식별자", required = true)
        @PathVariable @NotBlank(message = "Device ID는 필수입니다") deviceId: String
    ): ApiResponse<MemberStatusResponse> {
        val result = loginService.checkMemberStatus(deviceId)
        return ApiResponse.success(result)
    }


    @Operation(
        summary = "민감도 설정과 동시에 회원 생성",
        description = "신규 사용자의 회원 생성 + 5단계 민감도 설정을 원자적으로 처리합니다."
    )
    @PostMapping("/{deviceId}/setup-with-preference")
    fun setupWithPreference(
        @Parameter(description = "디바이스 고유 식별자", required = true)
        @PathVariable @NotBlank(message = "Device ID는 필수입니다") deviceId: String,
        @Valid @RequestBody request: WeatherPreferenceSetupRequest
    ): ApiResponse<WeatherPreferenceResponse> {
        val result = loginService.setupWeatherPreferenceWithMember(deviceId, request)
        return ApiResponse.success(result)
    }
}