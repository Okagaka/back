package com.okagaka.OkaGaka.domain.user.controller;

import com.okagaka.OkaGaka.common.response.ApiResponse;
import com.okagaka.OkaGaka.common.security.CustomUserDetails;
import com.okagaka.OkaGaka.domain.user.dto.EmbeddingImageUpdateRequest;
import com.okagaka.OkaGaka.domain.user.dto.UserFaceEmbeddingImageResponse;
import com.okagaka.OkaGaka.domain.user.dto.UserFaceImageResponse;
import com.okagaka.OkaGaka.domain.user.service.UserFaceImageService;
import com.sun.security.auth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-face-images")
public class UserFaceImageController {

    private final UserFaceImageService userFaceImageService;


    //현재 인증된 사용자의 얼굴 이미지 목록을 조회하는 API
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<UserFaceImageResponse>>> getMyFaceImages(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            Long currentUserId = userDetails.getUserId();

            List<UserFaceImageResponse> userImages = userFaceImageService.getUserFaceImages(currentUserId);

            return ResponseEntity.ok(ApiResponse.success(userImages));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("이미지 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PutMapping("/embedding")
    public ResponseEntity<ApiResponse<List<UserFaceEmbeddingImageResponse>>> updateEmbeddingImages(
            @RequestBody EmbeddingImageUpdateRequest request
    ) {
        try {
            List<UserFaceEmbeddingImageResponse> responses =
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
