package com.okagaka.OkaGaka.domain.signup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmbeddingItem {
    private Long id; // UserFaceImage의 ID
    private String embeddingUrl;
}
