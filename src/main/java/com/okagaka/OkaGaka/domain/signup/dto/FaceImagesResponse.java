package com.okagaka.OkaGaka.domain.signup.dto;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FaceImagesResponse {
    private List<String> faceImages;
}
