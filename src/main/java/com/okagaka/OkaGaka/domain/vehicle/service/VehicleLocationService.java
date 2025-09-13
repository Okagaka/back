package com.okagaka.OkaGaka.domain.vehicle.service;

import com.okagaka.OkaGaka.common.exception.CustomException;
import com.okagaka.OkaGaka.common.exception.ErrorCode;
import com.okagaka.OkaGaka.domain.familygroup.entity.FamilyGroup;
import com.okagaka.OkaGaka.domain.location.service.LocationCacheService;
import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.domain.user.repository.UserRepository;
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
import java.util.List;

@Service
public class VehicleLocationService {

    @Autowired
    private VehicleRepository vehicleRepository; // JPA Repository

    @Autowired
    private UserRepository userRepository;

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
//        messagingTemplate.convertAndSend("/topic/group/" + groupId, updateMessage);

        // ✅ 6. 수정된: 그룹에 속한 모든 사용자에게 1:1로 메시지 전송
        List<User> usersInGroup = userRepository.findAllByFamilyGroupId(groupId);

        for (User user : usersInGroup) {
            // CustomUserDetails를 사용하신다면 user.getId()가 맞습니다.
            // Spring Security의 Principal.getName()에 해당하는 사용자 고유 식별자를 사용해야 합니다.
            String username = String.valueOf(user.getId());

            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/location", // 클라이언트가 구독할 개인 큐 주소
                    updateMessage
            );
        }
    }

    // DTO 생성 로직 분리 (가독성을 위해)
    private VehicleLocationDTO createBroadcastDto(Long vehicleId, Long groupId, VehicleLocationUpdateDTO updateDto) {
        VehicleLocationDTO broadcastDto = new VehicleLocationDTO();
        broadcastDto.setVehicleId(vehicleId);
        broadcastDto.setGroupId(groupId);
        broadcastDto.setLatitude(updateDto.getLatitude());
        broadcastDto.setLongitude(updateDto.getLongitude());
        broadcastDto.setBatteryLevel(updateDto.getBatteryLevel());
        broadcastDto.setSpeed(updateDto.getSpeed());
        broadcastDto.setStatus(updateDto.getStatus());
        broadcastDto.setTimestamp(LocalDateTime.now());
        return broadcastDto;
    }

}

