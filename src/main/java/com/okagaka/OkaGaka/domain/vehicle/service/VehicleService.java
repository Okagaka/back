package com.okagaka.OkaGaka.domain.vehicle.service;

import com.okagaka.OkaGaka.common.exception.CustomException;
import com.okagaka.OkaGaka.common.exception.ErrorCode;
import com.okagaka.OkaGaka.domain.familygroup.entity.FamilyGroup;
import com.okagaka.OkaGaka.domain.familygroup.repository.FamilyGroupRepository;
import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.domain.user.repository.UserRepository;
import com.okagaka.OkaGaka.domain.vehicle.dto.VehicleRegisterRequest;
import com.okagaka.OkaGaka.domain.vehicle.dto.VehicleRegistrationResponse;
import com.okagaka.OkaGaka.domain.vehicle.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final UserRepository userRepository;
    private final FamilyGroupRepository familyGroupRepository; // FamilyGroup을 직접 다루므로 추가
    private final VehicleRegistrationService vehicleRegistrationService;  // API 키 생성 로직

    @Transactional
    public VehicleRegistrationResponse registerVehicleToGroup(Long userId, VehicleRegisterRequest request) {
        System.out.println("여기");
        // 1. 사용자 정보와 속한 가족 그룹 조회
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User user = userRepository.findByIdWithFamilyGroup(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        System.out.println("여기2");

        FamilyGroup familyGroup = user.getFamilyGroup();
        if (familyGroup == null) {
            // 이 경우는 발생하면 안되지만, 방어 코드
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 2. 해당 그룹에 이미 차량이 등록되었는지 확인 (1그룹 1차량 정책)
        if (familyGroup.getVehicle() != null) {
            System.out.println(familyGroup.getVehicle());
            throw new CustomException(ErrorCode.VEHICLE_ALREADY_EXISTS_IN_GROUP);
        }

        // 3. 차량 엔티티 생성 및 API 키 발급
        Vehicle newVehicle = Vehicle.builder()
                .model(request.getVehicleModel())
                .number(request.getVehicleNumber())
                .build();
        String rawApiKey = vehicleRegistrationService.registerNewVehicle(newVehicle); // 키 생성 및 해시값 DB 저장

        // 4. 가족 그룹에 생성된 차량 정보 연결
        familyGroup.setVehicle(newVehicle);
        familyGroupRepository.save(familyGroup); // 변경 감지가 동작하지만 명시적으로 save 호출

        // 5. 차량 정보와 '원본 API 키'를 사용자에게 반환
        return new VehicleRegistrationResponse(newVehicle.getId(), newVehicle.getModel(), newVehicle.getNumber(), rawApiKey);
    }

}
