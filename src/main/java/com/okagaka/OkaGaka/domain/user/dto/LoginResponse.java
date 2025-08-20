package com.okagaka.OkaGaka.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
}
