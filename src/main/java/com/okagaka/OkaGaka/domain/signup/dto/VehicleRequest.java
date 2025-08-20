package com.okagaka.OkaGaka.domain.signup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "회원가입 차량 정보 DTO")
public class VehicleRequest {
    @Schema(description = "차량 ID (가족 그룹이 없을 때만 사용)")
    private Long vehicleId;

    @Schema(description = "차량 모델명", example = "카니발")
    private String vehicleModel;
}
