package com.okagaka.OkaGaka.domain.vehicle.service;

import com.okagaka.OkaGaka.domain.vehicle.entity.Vehicle;
import com.okagaka.OkaGaka.domain.vehicle.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID;

@Service
public class VehicleRegistrationService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String registerNewVehicle(Vehicle newVehicle) {
        // 1. 랜덤 API 키 생성 (e.g., UUID)
        String rawApiKey = UUID.randomUUID().toString();

        // 2. API 키를 해싱
        String hashedApiKey = passwordEncoder.encode(rawApiKey);
        newVehicle.setApiKeyHash(hashedApiKey); // 해싱된 키를 엔티티에 설정

        // 3. DB에 저장
        vehicleRepository.save(newVehicle);

        // 4. 생성된 원본 키를 차량(물리적 장치)에 안전하게 전달
        return rawApiKey;
    }

}
