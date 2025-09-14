package com.okagaka.OkaGaka.domain.carrequest.dto;

public record CarRequestDto(
        String departureCityDo,
        String departureGuGun,
        String departureDong,
        String departureBunji,

        // 도착지
        String destinationCityDo,
        String destinationGuGun,
        String destinationDong,
        String destinationBunji
) { }
