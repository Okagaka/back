package com.okagaka.OkaGaka.domain.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RealTimeUpdate<T> {
    private String type; // "USER_UPDATE", "VEHICLE_UPDATE" 등
    private T payload;   // LocationDTO 또는 VehicleLocationDTO
}

