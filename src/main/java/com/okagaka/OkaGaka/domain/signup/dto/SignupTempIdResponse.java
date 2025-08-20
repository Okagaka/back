package com.okagaka.OkaGaka.domain.signup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "회원가입 임시 ID 응답 DTO")
public class SignupTempIdResponse {
    private Long tempId;
    private String name;
}
