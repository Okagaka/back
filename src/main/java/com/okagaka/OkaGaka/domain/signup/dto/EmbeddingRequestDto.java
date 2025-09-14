package com.okagaka.OkaGaka.domain.signup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;


@Getter
@AllArgsConstructor
public class EmbeddingRequestDto {
    private Long userId;
    private List<FaceImageInfo> images;
}
