package com.okagaka.OkaGaka.domain.vehicle.controller;

import com.okagaka.OkaGaka.domain.vehicle.entity.Vehicle;
import com.okagaka.OkaGaka.domain.vehicle.repository.VehicleRepository;
import com.okagaka.OkaGaka.domain.vehicle.service.VehicleLocationService;
import com.okagaka.OkaGaka.domain.vehicle.dto.VehicleLocationUpdateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleLocationController {

    @Autowired
    private VehicleLocationService vehicleLocationService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/{vehicleId}/location")
    public ResponseEntity<Void> updateVehicleLocation(
            @PathVariable Long vehicleId,
            @RequestBody VehicleLocationUpdateDTO dto,
            @RequestHeader("X-API-KEY") String apiKey) {

        // 1. API 키 유효성 검증
        if (!isValidApiKey(apiKey, vehicleId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. 서비스 호출하여 위치 정보 업데이트 및 브로드캐스팅
        vehicleLocationService.updateAndBroadcastLocation(vehicleId, dto);

        return ResponseEntity.ok().build();
    }

    private boolean isValidApiKey(String rawApiKey, Long vehicleId) {
        // 1. vehicleId로 차량 정보를 DB에서 조회
        Optional<Vehicle> vehicleOptional = vehicleRepository.findById(vehicleId);

        if (vehicleOptional.isEmpty()) {
            return false; // 해당 ID의 차량이 없음
        }

        // 2. DB에 저장된 해시값 가져오기
        String hashedApiKey = vehicleOptional.get().getApiKeyHash();

        // 3. 클라이언트가 보낸 원본 키와 DB의 해시값을 비교
        // passwordEncoder.matches(원본, 해시)
        return passwordEncoder.matches(rawApiKey, hashedApiKey);
    }
}
