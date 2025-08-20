package com.okagaka.OkaGaka.domain.signup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "회원가입 주소 등록 DTO")
public class AddressRequest {
    @Schema(description = "자주 이용하는 장소", example = "서울특별시 송파구")
    private String location;

}
