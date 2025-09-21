package com.okagaka.OkaGaka.domain.reservation.controller;

import com.okagaka.OkaGaka.common.response.ApiResponse;
import com.okagaka.OkaGaka.common.security.CustomUserDetails;
import com.okagaka.OkaGaka.domain.reservation.dto.CarpoolProposalResponse;
import com.okagaka.OkaGaka.domain.reservation.dto.ReservationListResponse;
import com.okagaka.OkaGaka.domain.reservation.service.ReservationService;
import com.okagaka.OkaGaka.domain.reservation.dto.ReservationResponse;
import com.okagaka.OkaGaka.domain.reservation.dto.ReservationRequest;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
@Tag(name = "Reservation API", description = "차량 예약 API")
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 차량 예약 등록
     * - 단독 예약 가능 시 바로 확정
     * - 겹치는 예약 존재 시 카풀 제안
     */
    @Operation(summary = "차량 예약 등록", description = "자동으로 출발시간 계산 + 충돌 시 카풀 제안")
    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @Valid @RequestBody ReservationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId(); // JWT에서 꺼낸 사용자 ID
        ReservationResponse response = reservationService.createReservation(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 카풀 제안 수락
     */
    @Operation(summary = "카풀 제안 수락", description = "기존 예약자가 카풀 제안을 수락")
    @PostMapping("/proposals/{proposalId}/accept")
    public ResponseEntity<ApiResponse<ReservationResponse>> acceptCarpoolProposal(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        ReservationResponse response = reservationService.acceptCarpoolProposal(proposalId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 카풀 제안 거절
     */
    @Operation(summary = "카풀 제안 거절", description = "기존 예약자가 카풀 제안을 거절")
    @PostMapping("/proposals/{proposalId}/reject")
    public ResponseEntity<ApiResponse<ReservationResponse>> rejectCarpoolProposal(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        ReservationResponse response = reservationService.rejectCarpoolProposal(proposalId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "카풀 제안이 거부되어 요청자 예약이 취소되었습니다."));
    }

    /**
     * 현재 사용자가 받은 카풀 제안 목록 조회
     */
    @Operation(summary = "받은 카풀 제안 조회", description = "사용자가 받은 PENDING 카풀 제안 목록 반환")
    @GetMapping("/proposals/received")
    public ResponseEntity<ApiResponse<List<CarpoolProposalResponse>>> getReceivedCarpoolProposals(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        List<CarpoolProposalResponse> proposals = reservationService.getReceivedCarpoolProposals(userId);
        return ResponseEntity.ok(ApiResponse.success(proposals));
    }

    /**
     * 로그인한 사용자의 예약 목록 조회
     */
    @Operation(summary = "내 예약 목록 조회", description = "로그인한 사용자의 모든 예약 정보를 최신순으로 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReservationListResponse>>> getUserReservations(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        List<ReservationListResponse> reservations = reservationService.getUserReservations(userId);
        return ResponseEntity.ok(ApiResponse.success(reservations));
    }


}
