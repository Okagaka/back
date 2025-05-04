package com.okagaka.OkaGaka.domain.vehicle.repository;

import com.okagaka.OkaGaka.domain.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
}
