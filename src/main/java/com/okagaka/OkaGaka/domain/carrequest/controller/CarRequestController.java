//package com.okagaka.OkaGaka.domain.carrequest.controller;
//
//import com.okagaka.OkaGaka.common.response.ApiResponse;
//import com.okagaka.OkaGaka.common.security.CustomUserDetails;
//import com.okagaka.OkaGaka.domain.carrequest.dto.CarRequestDto;
//import com.okagaka.OkaGaka.domain.carrequest.service.CarRequestService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/car-request")
//@Tag(name = "Car Request API", description = "차량 이용 요청 및 AI 추천 API")
//public class CarRequestController {
//    private final CarRequestService carRequestService;
//
//    @Operation(summary = "차량 이용 요청", description = "요청을 등록하고 AI의 추천 및 승인 절차를 시작합니다.")
//    @PostMapping
//    public ResponseEntity<ApiResponse<CarRequestResponseDto>> createCarReqeust(
//            @RequestBody CarRequestDto requestDto,
//            @AuthenticationPrincipal CustomUserDetails userDetails
//    ) {
//        Long userId = userDetails.getUserId();
//        CarRequestResponseDto response = carRequestService.createAndProcessCarRequest(userId, requestDto);
//        return ResponseEntity.accepted().body(ApiResponse.success(response)); // 202 Accepted 응답(요청은 받았지만 실제 처리는 비동기적으로 처리)
//    }
//
//
//}
