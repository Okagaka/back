package com.okagaka.OkaGaka.domain.signup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "회원가입 이름 등록 DTO")
public class NameRequest {

    @Schema(description = "사용자 이름", example = "홍길동")
    @NotBlank(message = "이름은 비어 있을 수 없습니다.")
    private String name;
}
