package com.okagaka.OkaGaka.domain.user.controller;

import org.springframework.web.bind.annotation.*;

@RestController
public class HealthCheckController {
    @GetMapping("/")
    public String home() {
        return "API 서버가 실행 중입니다.";
    }
}
