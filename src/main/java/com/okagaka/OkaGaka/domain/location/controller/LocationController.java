package com.okagaka.OkaGaka.domain.location.controller;

import com.okagaka.OkaGaka.domain.location.dto.LocationDTO;
import com.okagaka.OkaGaka.domain.location.service.LocationCacheService;
import com.okagaka.OkaGaka.domain.familygroup.service.FamilyGroupService;
import com.okagaka.OkaGaka.common.response.ApiResponse;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import com.okagaka.OkaGaka.common.exception.CustomException;
import com.okagaka.OkaGaka.common.exception.ErrorCode;
import com.okagaka.OkaGaka.common.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.security.Principal;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/groups")
public class LocationController {

    @Autowired
    private LocationCacheService cacheService;

    @Autowired
    private FamilyGroupService familyGroupService;


    // 현재 로그인한 사용자 위치 조회
    @GetMapping("/{groupId}/locations/me")
    public ResponseEntity<ApiResponse<LocationDTO>> getMyLocation(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();

        // 1. 내 위치 조회 권한 획득 (내가 해당 그룹 멤버인지)
        if (!familyGroupService.isUserInGroup(userId, groupId)) {
            throw new CustomException(ErrorCode.USER_UNAUTHORIZED);
        }

        // 2. Redis에서 내 위치 가져오기
        Optional<LocationDTO> myLocationOptional = cacheService.getUserLocation(groupId, userId);

        // 위치 정보 존재하는지 확인
        if (myLocationOptional.isPresent()) {
            LocationDTO myLocation = myLocationOptional.get();
            return ResponseEntity.ok(ApiResponse.success(myLocation));
        } else {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }


    // 현재 로그인한 사용자 가족 위치 조회
    @GetMapping("/{groupId}/locations")
    public ResponseEntity<ApiResponse<List<LocationDTO>>> getAllGroupLocations(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 1. 현재 요청하는 사용자가 해당 groupId의 멤버인지 확인 (인가)
        Long requestorUserId = userDetails.getUserId();

        if (!familyGroupService.isUserInGroup(requestorUserId, groupId)) {
            throw new CustomException(ErrorCode.USER_UNAUTHORIZED); // 권한이 없는 경우
        }

        // 2. 해당 groupId에 속한 모든 멤버의 ID 가져오기
        List<Long> memberIds = familyGroupService.getUserIdsById(groupId);
        System.out.println("memberIds: " + memberIds); // 디버깅

        // 3. Redis에서 각 멤버의 최신 위치 정보를 가져오기
        List<LocationDTO> locations = cacheService.getAllGroupLocations(groupId, memberIds);
        System.out.println("locations: " + locations); // 디버깅

        // 성공 응답 반환
        return ResponseEntity.ok(ApiResponse.success(locations));
    }
}
