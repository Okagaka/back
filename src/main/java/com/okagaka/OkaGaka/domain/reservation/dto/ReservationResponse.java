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
    private String message;
}
