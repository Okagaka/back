package com.okagaka.OkaGaka.domain.signup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmbeddingResponseDto {
    private int status;
    private String message;
    private EmbeddingData data;
}
