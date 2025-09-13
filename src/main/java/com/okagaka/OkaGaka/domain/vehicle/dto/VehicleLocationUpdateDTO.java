package com.okagaka.OkaGaka.domain.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.okagaka.OkaGaka.domain.vehicle.entity.Vehicle;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleLocationUpdateDTO {
    private Double latitude;
    private Double longitude;
    private Integer batteryLevel;
    private Float speed;
    private Vehicle.VehicleStatus status;

}
