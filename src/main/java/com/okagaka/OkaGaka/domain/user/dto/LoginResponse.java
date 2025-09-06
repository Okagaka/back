package com.okagaka.OkaGaka.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
//@AllArgsConstructor
@RequiredArgsConstructor
public class LoginResponse {
    private final String accessToken;
    private final Long userId;
    private final Long groupId;
}
