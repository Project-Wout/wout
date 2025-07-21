package com.wout.member.controller

import com.wout.common.response.ApiResponse
import com.wout.member.dto.mypage.request.LocationUpdateRequest
import com.wout.member.dto.mypage.request.NicknameUpdateRequest
import com.wout.member.dto.mypage.request.WeatherPreferenceUpdateRequest
import com.wout.member.dto.mypage.response.MemberResponse
import com.wout.member.dto.mypage.response.WeatherPreferenceResponse
import com.wout.member.service.MyPageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.*

/**
 * packageName    : com.wout.member.controller
 * fileName       : MyPageController
 * author         : MinKyu Park
 * date           : 25. 6. 18.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 6. 18.        MinKyu Park       최초 생성
 */
@RestController
@RequestMapping("/api/my")
@Tag(name = "마이페이지", description = "'마이 탭 기능' 관련 API")
class MyPageController(
    private val myPageService: MyPageService
) {

    @Operation(
        summary = "회원 정보 조회",
        description = "기본 회원 정보만 조회합니다."
    )
    @GetMapping("/{deviceId}/info")
    fun getMemberInfo(
        @Parameter(description = "디바이스 고유 식별자", required = true)
        @PathVariable @NotBlank(message = "Device ID는 필수입니다") deviceId: String
    ): ApiResponse<MemberResponse> {
        val result = myPageService.getMemberByDeviceId(deviceId)
        return ApiResponse.success(result)
    }

    @Operation(
        summary = "날씨 선호도 조회",
        description = "회원의 날씨 선호도 설정을 조회합니다."
    )
    @GetMapping("/{deviceId}/weather-preference")
    fun getWeatherPreference(
        @Parameter(description = "디바이스 고유 식별자", required = true)
        @PathVariable @NotBlank(message = "Device ID는 필수입니다") deviceId: String
    ): ApiResponse<WeatherPreferenceResponse> {
        val result = myPageService.getWeatherPreference(deviceId)
        return ApiResponse.success(result)
    }

    @Operation(
        summary = "닉네임 수정",
        description = "회원의 닉네임을 수정합니다."
    )
    @PatchMapping("/{deviceId}/nickname")
    fun updateNickname(
        @Parameter(description = "디바이스 고유 식별자", required = true)
        @PathVariable @NotBlank(message = "Device ID는 필수입니다") deviceId: String,
        request: NicknameUpdateRequest
    ): ApiResponse<MemberResponse> {
        val result = myPageService.updateNickname(deviceId, request.nickname)
        return ApiResponse.success(result)
    }

    @Operation(
        summary = "기본 위치 정보 수정",
        description = "회원의 기본 위치 정보를 수정합니다."
    )
    @PatchMapping("/{deviceId}/location")
    fun updateLocation(
        @Parameter(description = "디바이스 고유 식별자", required = true)
        @PathVariable @NotBlank(message = "Device ID는 필수입니다") deviceId: String,
        @Valid @RequestBody request: LocationUpdateRequest
    ): ApiResponse<MemberResponse> {
        val result = myPageService.updateLocation(deviceId, request)
        return ApiResponse.success(result)
    }


    @Operation(
        summary = "날씨 선호도 수정",
        description = "기존 회원의 날씨 선호도를 수정합니다. (마이페이지 → 민감도 재조정용)"
    )
    @PutMapping("/{deviceId}/weather-preference")
    fun updateWeatherPreference(
        @Parameter(description = "디바이스 고유 식별자", required = true)
        @PathVariable @NotBlank(message = "Device ID는 필수입니다") deviceId: String,
        @Valid @RequestBody request: WeatherPreferenceUpdateRequest
    ): ApiResponse<WeatherPreferenceResponse> {
        val result = myPageService.updateWeatherPreference(deviceId, request)
        return ApiResponse.success(result)
    }


    @Operation(
        summary = "회원 탈퇴",
        description = "회원 계정을 비활성화합니다."
    )
    @DeleteMapping("/{deviceId}")
    fun deactivateMember(
        @Parameter(description = "디바이스 고유 식별자", required = true)
        @PathVariable @NotBlank(message = "Device ID는 필수입니다") deviceId: String
    ): ApiResponse<MemberResponse> {
        val result = myPageService.deactivateMember(deviceId)
        return ApiResponse.success(result)
    }
}