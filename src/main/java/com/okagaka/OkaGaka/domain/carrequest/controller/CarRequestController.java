package com.okagaka.OkaGaka.domain.carrequest.controller;

import com.okagaka.OkaGaka.common.response.ApiResponse;
import com.okagaka.OkaGaka.common.security.CustomUserDetails;
import com.okagaka.OkaGaka.domain.aidecision.dto.AiDecisionResponse;
import com.okagaka.OkaGaka.domain.carrequest.dto.CarRequestDto;
import com.okagaka.OkaGaka.domain.carrequest.dto.CarRequestListResponse;
import com.okagaka.OkaGaka.domain.carrequest.dto.CarRequestResponse;
import com.okagaka.OkaGaka.domain.carrequest.dto.ConfirmationRequest;
import com.okagaka.OkaGaka.domain.carrequest.service.CarRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/car-request")
@Tag(name = "Car Request API", description = "차량 이용 요청 및 AI 추천 API")
public class CarRequestController {
    private final CarRequestService carRequestService;
    private static final Logger log = LoggerFactory.getLogger(CarRequestController.class);

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

    @Operation(summary = "사용자 최종 선택 확정", description = "AI의 분석 결과를 보고 사용자가 내린 최종 결정을 서버에 전송합니다.")
    @PostMapping("/{carRequestId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmChoice(
            @PathVariable Long carRequestId,
            @RequestBody ConfirmationRequest confirmationDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 로그 추가: API 호출 시점의 파라미터 기록
        log.info(">>>>> [API CALL] confirmChoice 호출 - carRequestId: {}, userId: {}, choice: {}",
                carRequestId, userDetails.getUserId(), confirmationDto.choice());
        carRequestService.confirmUserChoice(carRequestId, userDetails.getUserId(), confirmationDto);
        return ResponseEntity.ok(ApiResponse.success(null, "결정이 성공적으로 반영되었습니다."));
    }

    @Operation(summary = "내 차량 요청 목록 조회", description = "로그인한 사용자의 모든 차량 요청 목록을 최신순으로 조회합니다.")
    @GetMapping("/my-requests")
    public ResponseEntity<ApiResponse<List<CarRequestListResponse>>> getMyCarRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        List<CarRequestListResponse> response = carRequestService.getUserCarRequests(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
