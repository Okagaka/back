package com.okagaka.OkaGaka.domain.reservation.dto;

import com.okagaka.OkaGaka.domain.reservation.entity.Reservation;
import com.okagaka.OkaGaka.domain.reservation.enums.ReservationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReservationListResponse {

    private Long reservationId;
    private String title;

    private String departureAddress;
    private String destinationAddress;

    private ReservationStatus status;
    private LocalDateTime departureDateTime;
    private LocalDateTime arrivalDateTime;

//    // Reservation 엔티티를 받아 ReservationListResponse 객체를 생성하는 정적 메서드
//    public static ReservationListResponse from(Reservation reservation) {
//        return ReservationListResponse.builder()
//                .reservationId(reservation.getId())
//                .title(reservation.getTitle())
//                .status(reservation.getStatus())
//                .departureDateTime(reservation.getDepartureDateTime())
//                .arrivalDateTime(reservation.getArrivalDateTime())
//                .build();
//    }

}
