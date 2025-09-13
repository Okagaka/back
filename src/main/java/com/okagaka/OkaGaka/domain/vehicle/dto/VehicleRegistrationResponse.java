package com.okagaka.OkaGaka.domain.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VehicleRegistrationResponse {
    private Long vehicleId;
    private String vehicleModel;
    private String vehicleNumber;
    private String apiKey; // 사용자에게 1회만 보여줄 원본 API 키
}
