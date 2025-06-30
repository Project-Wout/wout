package com.wout.outfit.controller

import com.wout.common.response.ApiResponse
import com.wout.outfit.dto.request.OutfitRecommendationRequest
import com.wout.outfit.dto.response.OutfitRecommendationResponse
import com.wout.outfit.dto.response.OutfitRecommendationSummary
import com.wout.outfit.service.OutfitRecommendationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

/**
 * packageName    : com.wout.outfit.controller
 * fileName       : OutfitController
 * author         : MinKyu Park
 * date           : 2025-06-02
 * description    : 아웃핏 추천 API 컨트롤러 (MVP 1차 범위)
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-06-02        MinKyu Park       최초 생성 (MVP 핵심 기능만)
 * 2025-06-03        MinKyu Park       서비스 메서드와 정확히 매칭
 * 2025-06-04        MinKyu Park       실제 구현된 서비스 메서드 연동
 */
@RestController
@RequestMapping("/api/outfit")
@Tag(name = "Outfit Recommendation API", description = "개인화된 아웃핏 추천 시스템 API")
@Validated
class OutfitController(
    private val outfitRecommendationService: OutfitRecommendationService
) {

    @Operation(
        summary = "개인화된 아웃핏 추천 생성",
        description = "사용자의 날씨 민감도와 현재 날씨를 기반으로 맞춤형 아웃핏을 추천합니다. " +
                "1시간 이내 동일한 날씨 데이터로 추천이 있으면 기존 추천을 반환합니다."
    )
    @PostMapping("/recommend")
    fun generateOutfitRecommendation(
        @Valid @RequestBody request: OutfitRecommendationRequest
    ): ApiResponse<OutfitRecommendationResponse> {
        val result = outfitRecommendationService.generatePersonalizedOutfitRecommendation(
            deviceId = request.deviceId,
            weatherDataId = request.weatherDataId
        )
        return ApiResponse.success(result)
    }

    @Operation(
        summary = "추천 히스토리 조회",
        description = "사용자의 최근 아웃핏 추천 히스토리를 조회합니다. " +
                "기본 10개까지 조회되며, limit 파라미터로 조회 개수를 조정할 수 있습니다."
    )
    @GetMapping("/{deviceId}/history")
    fun getRecommendationHistory(
        @Parameter(description = "디바이스 ID", required = true)
        @PathVariable deviceId: String,
        @Parameter(description = "조회할 추천 개수 (기본: 10개, 최대: 100개)")
        @RequestParam(defaultValue = "10") @Positive @Max(100, message = "조회 개수는 최대 100개까지 가능합니다") limit: Int
    ): ApiResponse<List<OutfitRecommendationSummary>> {
        val results = outfitRecommendationService.getRecommendationHistory(deviceId, limit)
        return ApiResponse.success(results)
    }

    @Operation(
        summary = "추천 상세 정보 조회",
        description = "특정 추천의 상세 정보를 조회합니다. 본인의 추천만 조회할 수 있습니다."
    )
    @GetMapping("/{deviceId}/recommendations/{recommendationId}")
    fun getRecommendationDetail(
        @Parameter(description = "디바이스 ID", required = true)
        @PathVariable deviceId: String,
        @Parameter(description = "추천 ID", required = true)
        @PathVariable @Positive(message = "추천 ID는 양수여야 합니다") recommendationId: Long
    ): ApiResponse<OutfitRecommendationResponse> {
        val result = outfitRecommendationService.getRecommendationDetail(deviceId, recommendationId)
        return ApiResponse.success(result)
    }
}