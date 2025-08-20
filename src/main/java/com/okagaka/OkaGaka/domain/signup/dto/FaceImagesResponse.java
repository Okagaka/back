package com.okagaka.OkaGaka.domain.signup.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceImagesResponse {
    private List<String> imageUrls;
}
