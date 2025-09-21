package com.okagaka.OkaGaka.domain.vehicle.dto;

import com.okagaka.OkaGaka.domain.vehicle.entity.Vehicle;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleLocationDTO {
    private Long vehicleId;
    private Long groupId;
    private Double latitude;
    private Double longitude;
    private Integer batteryLevel = 100;
    private Float speed = 60.5f;
    private Vehicle.VehicleStatus status;
    private LocalDateTime timestamp;

}
