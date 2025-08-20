package com.okagaka.OkaGaka.domain.reservation.controller;

import com.okagaka.OkaGaka.common.response.ApiResponse;
import com.okagaka.OkaGaka.domain.reservation.service.ReservationService;
import com.okagaka.OkaGaka.domain.reservation.dto.ReservationResponse;
import com.okagaka.OkaGaka.domain.reservation.dto.ReservationRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
@Tag(name = "Reservation API", description = "차량 예약 API")
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "차량 예약 등록", description = "자동으로 출발시간 계산 + 충돌 시 카풀 제안")
    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(@Valid @RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }



}
