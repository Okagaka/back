package com.okagaka.OkaGaka.domain.stt.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import com.okagaka.OkaGaka.common.utils.AudioUtil;
import com.okagaka.OkaGaka.domain.user.repository.UserRepository;
import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.domain.stt.service.SpeechToTextService;
import com.okagaka.OkaGaka.common.response.ApiResponse;
import com.okagaka.OkaGaka.common.security.JwtTokenProvider;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;


import java.io.File;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stt")
public class SpeechToTextController {

    private final SpeechToTextService speechToTextService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<String>> uploadAudioAndRecognize(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        File tempFile = null;
        File monoFile = null;

        try {

            // 0. 토큰 유효성 검증
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(ApiResponse.error("토큰이 없습니다."));
            }

            String token = authorizationHeader.substring(7); // "Bearer " 제거
            if (!jwtTokenProvider.validateToken(token)) {
                return ResponseEntity.status(401).body(ApiResponse.error("유효하지 않은 토큰입니다."));
            }

            Long userIdFromToken = jwtTokenProvider.getUserId(token);
            if (!userIdFromToken.equals(userId)) {
                return ResponseEntity.status(403).body(ApiResponse.error("권한이 없습니다."));
            }

            // 1. 파일 확장자 체크
            if (!file.getOriginalFilename().toLowerCase().endsWith(".wav")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("지원하지 않는 파일 형식입니다. .wav 파일만 허용됩니다."));
            }


            // 2. 임시 파일 저장
            tempFile = File.createTempFile("speech_", ".wav");
            file.transferTo(tempFile);

            // stereo면 mono로 변환
            monoFile = AudioUtil.convertToMonoIfStereo(tempFile);


            // 3. 음성 인식
            String recognizedText = speechToTextService.recognizeFromFile(monoFile.getAbsolutePath());


            // 4. 사용자 조회 및 상태 업데이트
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
            user.updateCondition(recognizedText);


            // 5. 성공 응답
            return ResponseEntity.ok(ApiResponse.success(recognizedText));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("STT 처리 중 오류가 발생했습니다: " + e.getMessage()));

        } finally {
//            if (tempFile != null && tempFile.exists()) {
//                tempFile.delete();
//            }
            if (tempFile != null && tempFile.exists()) tempFile.delete();
            if (monoFile != null && !monoFile.equals(tempFile) && monoFile.exists()) monoFile.delete();
        }
    }
}


