package com.okagaka.OkaGaka.domain.reservation.dto;

import com.okagaka.OkaGaka.domain.reservation.enums.ReservationStatus;

import java.time.LocalTime;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ReservationResponse {
    private Long reservationId;
    private ReservationStatus status;
    private LocalDateTime calculatedDepartureTime;

    private LocalDateTime desiredArrivalTime; // 사용자가 원했던 희망 도착 시간
    private LocalDateTime calculatedArrivalTime; // 카풀로 인해 계산된 최종 도착 시간

    private String message;
}
