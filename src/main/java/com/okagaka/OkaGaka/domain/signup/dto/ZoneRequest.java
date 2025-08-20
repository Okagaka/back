package com.okagaka.OkaGaka.domain.signup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "회원가입 영역 정보 DTO")
public class ZoneRequest {

//    @NotBlank(message = "영역 이름은 필수입니다.")
//    private String name;
//
////    @NotNull(message = "주소 정보는 필수입니다.")
//    @NotBlank(message = "주소 정보는 필수입니다.")
//    private String coordinates;  // JSON 문자열 또는 좌표 정보

    private String name;
//    private String coordinates;

    private String cityDo;  // 시/도
    private String guGun;   // 구/군
    private String dong;    // 도로명
    private String bunji;   // 번지
}
