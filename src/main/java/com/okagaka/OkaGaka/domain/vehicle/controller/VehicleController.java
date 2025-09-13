package com.okagaka.OkaGaka.domain.vehicle.controller;

import com.okagaka.OkaGaka.common.response.ApiResponse;
import com.okagaka.OkaGaka.common.security.CustomUserDetails;
import com.okagaka.OkaGaka.domain.vehicle.dto.VehicleRegisterRequest;
import com.okagaka.OkaGaka.domain.vehicle.dto.VehicleRegistrationResponse;
import com.okagaka.OkaGaka.domain.vehicle.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @Operation(summary = "가족 그룹에 차량 등록", description = "로그인한 사용자가 속한 가족 그룹에 차량을 등록하고 API 키를 발급받습니다.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<VehicleRegistrationResponse>> registerVehicle(
            @RequestBody @Valid VehicleRegisterRequest request,
            @AuthenticationPrincipal CustomUserDetails  userDetails) { // Spring Security로 로그인 사용자 정보 가져오기


        Long userId = userDetails.getUserId();

        VehicleRegistrationResponse response = vehicleService.registerVehicleToGroup(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


}
