package com.okagaka.OkaGaka.domain.user.service;

import com.okagaka.OkaGaka.common.exception.CustomException;
import com.okagaka.OkaGaka.common.exception.ErrorCode;
import com.okagaka.OkaGaka.domain.user.dto.EmbeddingImageUpdateRequest;
import com.okagaka.OkaGaka.domain.user.dto.UserFaceImageResponse;
import com.okagaka.OkaGaka.domain.user.repository.UserFaceImageRepository;
import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.domain.user.entity.UserFaceImage;
import com.okagaka.OkaGaka.domain.user.repository.UserRepository;
import com.okagaka.OkaGaka.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserFaceImageService {

    private final UserFaceImageRepository userFaceImageRepository;

    @Transactional
    public List<UserFaceImageResponse> updateEmbeddingImages(List<EmbeddingImageUpdateRequest.EmbeddingItem> embeddings) {
        // 1. 요청받은 ID 목록을 추출합니다.
        List<Long> ids = embeddings.stream()
                .map(EmbeddingImageUpdateRequest.EmbeddingItem::getId)
                .collect(Collectors.toList());

        // 2. ID 목록을 사용하여 모든 엔티티를 한 번의 쿼리로 조회합니다. (N+1 문제 해결)
        List<UserFaceImage> faceImages = userFaceImageRepository.findAllById(ids);

        // 3. (선택적) 요청된 ID의 수와 실제 DB에서 조회된 엔티티의 수가 같은지 검증합니다.
        if (faceImages.size() != ids.size()) {
            throw new IllegalArgumentException("요청된 ID 중 일부가 존재하지 않습니다.");
        }

        // 4. 빠른 조회를 위해 Map으로 변환합니다. (ID -> EmbeddingItem)
        Map<Long, EmbeddingImageUpdateRequest.EmbeddingItem> embeddingMap = embeddings.stream()
                .collect(Collectors.toMap(EmbeddingImageUpdateRequest.EmbeddingItem::getId, item -> item));

        // 5. 조회된 엔티티들의 값을 업데이트합니다.
        for (UserFaceImage faceImage : faceImages) {
            EmbeddingImageUpdateRequest.EmbeddingItem item = embeddingMap.get(faceImage.getId());
            if (item != null) {
                // 엔티티에 정의된 비즈니스 메서드 사용을 권장
                faceImage.updateEmbeddingImageUrl(item.getEmbeddingImageUrl());
            }
        }

        // 6. 변경된 엔티티들을 Response DTO로 변환하여 반환합니다.
        // JPA의 더티 체킹에 의해 이 메서드가 종료될 때 UPDATE 쿼리가 자동으로 실행됩니다.
        return faceImages.stream()
                .map(faceImage -> new UserFaceImageResponse(
                        faceImage.getId(),
                        faceImage.getImageUrl(),
                        faceImage.getEmbeddingImageUrl()
                ))
                .collect(Collectors.toList());
    }

//    private final UserFaceImageRepository userFaceImageRepository;
//
//    @Transactional
//    public List<UserFaceImageResponse> updateEmbeddingImages(List<EmbeddingImageUpdateRequest.EmbeddingItem> embeddings) {
//        List<UserFaceImageResponse> responses = new ArrayList<>();
//
//        for (EmbeddingImageUpdateRequest.EmbeddingItem item : embeddings) {
//            UserFaceImage faceImage = userFaceImageRepository.findById(item.getId())
//                    .orElseThrow(() -> new IllegalArgumentException("해당 ID의 이미지가 존재하지 않습니다: " + item.getId()));
//
//            // 값 업데이트
//            faceImage.setEmbeddingImageUrl(item.getEmbeddingImageUrl());
//
//            responses.add(new UserFaceImageResponse(
//                    faceImage.getId(),
//                    faceImage.getImageUrl(),
//                    faceImage.getEmbeddingImageUrl()
//            ));
//        }
//
//        return responses;
//    }
}
