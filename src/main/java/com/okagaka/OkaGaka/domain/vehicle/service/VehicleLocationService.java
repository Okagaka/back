package com.okagaka.OkaGaka.domain.vehicle.service;

import com.okagaka.OkaGaka.common.exception.CustomException;
import com.okagaka.OkaGaka.common.exception.ErrorCode;
import com.okagaka.OkaGaka.domain.familygroup.entity.FamilyGroup;
import com.okagaka.OkaGaka.domain.location.service.LocationCacheService;
import com.okagaka.OkaGaka.domain.vehicle.dto.RealTimeUpdate;
import com.okagaka.OkaGaka.domain.vehicle.dto.VehicleLocationDTO;
import com.okagaka.OkaGaka.domain.vehicle.dto.VehicleLocationUpdateDTO;
import com.okagaka.OkaGaka.domain.vehicle.entity.Vehicle;
import com.okagaka.OkaGaka.domain.vehicle.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class VehicleLocationService {

    @Autowired
    private VehicleRepository vehicleRepository; // JPA Repository

    @Autowired
    private LocationCacheService locationCacheService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;


    @Transactional
    public void updateAndBroadcastLocation(Long vehicleId, VehicleLocationUpdateDTO updateDto) {
        // 1. DB에서 차량 정보 조회 (FamilyGroup 정보가 필요)
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new CustomException(ErrorCode.VEHICLE_NOT_FOUND));

        FamilyGroup familyGroup = vehicle.getFamilyGroup();
        if (familyGroup == null) {
            // 차량이 그룹에 속해있지 않으면 브로드캐스팅할 수 없음
            return;
        }
        Long groupId = familyGroup.getId();

        // 2. RDB에 차량 상태 업데이트 (필요시)
        // DTO의 정보로 vehicle 엔티티의 상태(속도, 배터리 등)를 업데이트 할 수 있습니다.
        // vehicle.updateStatus(updateDto);
        // vehicleRepository.save(vehicle);


        // 3. Redis에 저장하고 클라이언트로 보낼 DTO 생성
        VehicleLocationDTO broadcastDto = new VehicleLocationDTO();
        broadcastDto.setVehicleId(vehicleId);
        broadcastDto.setGroupId(groupId);
        broadcastDto.setLatitude(updateDto.getLatitude());
        broadcastDto.setLongitude(updateDto.getLongitude());
        broadcastDto.setBatteryLevel(updateDto.getBatteryLevel());
        broadcastDto.setSpeed(updateDto.getSpeed());
        broadcastDto.setStatus(updateDto.getStatus());
        broadcastDto.setTimestamp(LocalDateTime.now());


        // 4. Redis에 최신 위치 저장
        locationCacheService.saveVehicleLocation(groupId, vehicleId, broadcastDto);


        // 5. 클라이언트에게 보낼 통합 메시지 생성
        // 클라이언트가 사용자 위치와 차량 위치를 쉽게 구분하도록 Wrapper DTO 사용을 권장합니다.
        RealTimeUpdate<VehicleLocationDTO> updateMessage = new RealTimeUpdate<>("VEHICLE_UPDATE", broadcastDto);


        // 6. 해당 그룹의 토픽으로 브로드캐스팅
        messagingTemplate.convertAndSend("/topic/group/" + groupId, updateMessage);
    }
}

