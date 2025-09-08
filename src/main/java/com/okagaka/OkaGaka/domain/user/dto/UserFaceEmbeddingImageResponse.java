package com.okagaka.OkaGaka.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserFaceEmbeddingImageResponse {
    private Long id;
    private String imageUrl;
    private String embeddingImageUrl;
}
