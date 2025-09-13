package com.okagaka.OkaGaka.domain.signup.controller;

import com.okagaka.OkaGaka.common.response.ApiResponse;
import com.okagaka.OkaGaka.domain.signup.dto.*;
import com.okagaka.OkaGaka.domain.signup.service.SignupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/signup")
@RequiredArgsConstructor
@Tag(name = "User API", description = "사용자 관련 API")
public class SignupController {

    private final SignupService signupService;

    @Operation(summary = "회원가입(이름 등록)", description = "사용자 이름 등록")
    @PostMapping("/name")
    public ResponseEntity<ApiResponse<SignupTempIdResponse>> registerName(@Valid @RequestBody NameRequest request) {
        Long tempId = signupService.saveName(request.getName());
        return ResponseEntity.ok(
                ApiResponse.success(
                        //new SignupTempIdResponse(tempId)
                        SignupTempIdResponse.builder()
                                .tempId(tempId)
                                .name(request.getName())
                                .build()
                ));
    }

    @Operation(summary = "회원가입(전화번호 등록)", description = "사용자 전화번호 등록")
    @PostMapping("/phone")
    public ResponseEntity<ApiResponse<PhoneResponse>> registerPhone(@RequestParam Long tempId, @Valid @RequestBody PhoneRequest request) {
        signupService.savePhone(tempId, request.getPhoneNumber());
        PhoneResponse response = new PhoneResponse(request.getPhoneNumber());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "회원가입(얼굴 이미지 4장 등록)", description = "사용자 얼굴 이미지 등록")
    @PostMapping(value = "/faces", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FaceImagesResponse>> registerFaceImages(
            @RequestParam Long tempId,
            @RequestParam("imageUrls") List<MultipartFile> imageUrls
    ) {
        List<String> uploadedImageUrls = signupService.saveFaceImages(tempId, imageUrls);
        FaceImagesResponse response = new FaceImagesResponse(uploadedImageUrls);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "회원가입(영역 정보 등록)", description = "사용자의 영역(Zone) 이름과 주소 등록")
    @PostMapping("/zone")
    public ResponseEntity<ApiResponse<ZoneResponse>> registerZone(@RequestParam Long tempId, @RequestBody ZoneRequest request) {
        ZoneResponse savedZone = signupService.saveZone(tempId, request);
        return ResponseEntity.ok(ApiResponse.success(savedZone));
    }

    @Operation(summary = "가족 그룹 검색", description = "가족 그룹 존재 여부 확인 및 ID 반환")
    @PostMapping("/family/search")
    public ResponseEntity<ApiResponse<FamilySearchResponse>> searchFamily(@RequestParam Long tempId, @Valid @RequestBody FamilyNameRequest request) {
        FamilySearchResponse response = signupService.searchFamilyGroup(tempId, request.getFamilyName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @Operation(summary = "가족 그룹 생성", description = "가족 그룹 신규 생성")
    @PostMapping("/family/create")
    public ResponseEntity<ApiResponse<FamilyCreateResponse>> createFamily(
            @RequestParam Long tempId,
            @RequestBody FamilyCreateRequest request
    ) {
        FamilyCreateResponse response = signupService.createFamilyGroup(
                tempId,
                request.getFamilyName(),
//                request.getVehicleModel(),
                request.getCityDo(),
                request.getGuGun(),
                request.getDong(),
                request.getBunji()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @Operation(summary = "회원가입 완료", description = "임시 가입 정보를 기반으로 실제 회원 생성")
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<SignupCompleteResponse>> completeSignup(@RequestParam Long tempId) {
        SignupCompleteResponse response = signupService.finalizeSignup(tempId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}

