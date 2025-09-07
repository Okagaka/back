package com.okagaka.OkaGaka.domain.reservation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationRequest {
//    private Long userId;
    private String title;
    private LocalDate date;
//    private LocalTime arrivalTime;
    private LocalTime desiredArrivalTime;

    //    private String departure;
    // 출발지
    private String departureCityDo;
    private String departureGuGun;
    private String departureDong;
    private String departureBunji;

//    private String destination;
    // 도착지
    private String destinationCityDo;
    private String destinationGuGun;
    private String destinationDong;
    private String destinationBunji;
}
