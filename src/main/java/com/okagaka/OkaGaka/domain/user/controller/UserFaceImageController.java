package com.okagaka.OkaGaka.domain.user.controller;

import com.okagaka.OkaGaka.common.response.ApiResponse;
import com.okagaka.OkaGaka.common.security.CustomUserDetails;
import com.okagaka.OkaGaka.domain.user.dto.EmbeddingImageUpdateRequest;
import com.okagaka.OkaGaka.domain.user.dto.UserFaceImageResponse;
import com.okagaka.OkaGaka.domain.user.service.UserFaceImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-face-embedding-images")
public class UserFaceImageController {

    private final UserFaceImageService userFaceImageService;

    @PutMapping("/embedding")
    public ResponseEntity<ApiResponse<List<UserFaceImageResponse>>> updateEmbeddingImages(
            @RequestBody EmbeddingImageUpdateRequest request
    ) {
        try {
            List<UserFaceImageResponse> responses =
                    userFaceImageService.updateEmbeddingImages(request.getEmbeddings());

            return ResponseEntity.ok(ApiResponse.success(responses));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Embedding 이미지 업데이트 중 오류: " + e.getMessage()));
        }
    }
}
