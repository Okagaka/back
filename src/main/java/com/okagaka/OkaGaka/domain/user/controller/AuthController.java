package com.okagaka.OkaGaka.domain.user.controller;

import com.okagaka.OkaGaka.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.okagaka.OkaGaka.domain.user.service.UserService;
import com.okagaka.OkaGaka.domain.user.dto.LoginRequest;
import com.okagaka.OkaGaka.domain.user.dto.LoginResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        System.out.println("요청 들어옴: name=" + request.getName() + ", phone=" + request.getPhoneNumber());
//        String token = userService.login(request.getName(), request.getPhoneNumber());
//        return ResponseEntity.ok(ApiResponse.success(new LoginResponse(token)));
        // 1. UserService로부터 3가지 정보가 담긴 LoginResult 객체를 받음
        UserService.LoginResult loginResult = userService.login(request.getName(), request.getPhoneNumber());

        // 2. LoginResult 객체의 정보로 최종 응답 객체인 LoginResponse를 생성
        LoginResponse response = new LoginResponse(
                loginResult.token(),
                loginResult.userId(),
                loginResult.groupId()
        );

        // 3. 성공 응답 반환
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

