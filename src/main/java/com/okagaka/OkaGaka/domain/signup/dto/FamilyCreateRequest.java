package com.okagaka.OkaGaka.domain.signup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "회원가입 가족 DTO")
public class FamilyCreateRequest {
    @NotBlank(message = "가족 그룹명은 필수입니다.")
    private String familyName;

//    @NotBlank(message = "차량 모델명은 필수입니다.")
//    private String vehicleModel;

//    @Schema(description = "집 좌표 (예: 37.12345,127.54321)")
//    @NotBlank(message = "집 좌표는 필수입니다.")
//    private String homeCoordinates;

    private String cityDo;  // 시/도
    private String guGun;   // 구/군
    private String dong;    // 도로명
    private String bunji;   // 번지

}
