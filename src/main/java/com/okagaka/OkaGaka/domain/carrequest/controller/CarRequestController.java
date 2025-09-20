package com.okagaka.OkaGaka.domain.carrequest.controller;

import com.okagaka.OkaGaka.common.response.ApiResponse;
import com.okagaka.OkaGaka.common.security.CustomUserDetails;
import com.okagaka.OkaGaka.domain.aidecision.dto.AiDecisionResponse;
import com.okagaka.OkaGaka.domain.carrequest.dto.CarRequestDto;
import com.okagaka.OkaGaka.domain.carrequest.dto.CarRequestResponse;
import com.okagaka.OkaGaka.domain.carrequest.service.CarRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/car-request")
@Tag(name = "Car Request API", description = "차량 이용 요청 및 AI 추천 API")
public class CarRequestController {
    private final CarRequestService carRequestService;

    @Operation(summary = "차량 이용 요청", description = "요청을 등록하고 AI의 추천 및 승인 절차를 시작합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<CarRequestResponse>> createCarRequest(
            @RequestBody CarRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        CarRequestResponse response = carRequestService.createAndProcessCarRequest(userId, requestDto);
        return ResponseEntity.accepted().body(ApiResponse.success(response)); // 202 Accepted 응답(요청은 받았지만 실제 처리는 비동기적으로 처리)
    }

    @Operation(summary = "AI 분석 결과 조회", description = "차량 요청에 대한 AI의 최종 분석 결과를 조회합니다.")
    @GetMapping("/{carRequestId}/decision")
    public ResponseEntity<ApiResponse<AiDecisionResponse>> getDecisionResult(
            @PathVariable Long carRequestId
    ) {
        AiDecisionResponse response = carRequestService.getDecisionResult(carRequestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
