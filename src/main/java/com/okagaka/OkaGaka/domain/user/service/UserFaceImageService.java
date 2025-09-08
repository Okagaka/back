package com.okagaka.OkaGaka.domain.user.service;

import com.okagaka.OkaGaka.domain.user.dto.EmbeddingImageUpdateRequest;
import com.okagaka.OkaGaka.domain.user.dto.UserFaceEmbeddingImageResponse;
import com.okagaka.OkaGaka.domain.user.dto.UserFaceImageResponse;
import com.okagaka.OkaGaka.domain.user.repository.UserFaceImageRepository;
import com.okagaka.OkaGaka.domain.user.entity.UserFaceImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserFaceImageService {

    private final UserFaceImageRepository userFaceImageRepository;

    @Transactional
    public List<UserFaceEmbeddingImageResponse> updateEmbeddingImages(List<EmbeddingImageUpdateRequest.EmbeddingItem> embeddings) {
        // 1. 요청받은 ID 목록 추출
        List<Long> ids = embeddings.stream()
                .map(EmbeddingImageUpdateRequest.EmbeddingItem::getId)
                .collect(Collectors.toList());

        // 2. ID 목록을 사용하여 모든 엔티티를 한 번의 쿼리로 조회 (N+1 문제 해결)
        List<UserFaceImage> faceImages = userFaceImageRepository.findAllById(ids);

        // 3. 요청된 ID의 수와 실제 DB에서 조회된 엔티티의 수가 같은지 검증
        if (faceImages.size() != ids.size()) {
            throw new IllegalArgumentException("요청된 ID 중 일부가 존재하지 않습니다.");
        }

        // 4. 빠른 조회를 위해 Map으로 변환(ID -> EmbeddingItem)
        Map<Long, EmbeddingImageUpdateRequest.EmbeddingItem> embeddingMap = embeddings.stream()
                .collect(Collectors.toMap(EmbeddingImageUpdateRequest.EmbeddingItem::getId, item -> item));

        // 5. 조회된 엔티티들의 값을 업데이트
        for (UserFaceImage faceImage : faceImages) {
            EmbeddingImageUpdateRequest.EmbeddingItem item = embeddingMap.get(faceImage.getId());
            if (item != null) {
                // 엔티티에 정의된 비즈니스 메서드 사용을 권장
                faceImage.updateEmbeddingImageUrl(item.getEmbeddingImageUrl());
            }
        }

        return faceImages.stream()
                .map(faceImage -> new UserFaceEmbeddingImageResponse(
                        faceImage.getId(),
                        faceImage.getImageUrl(),
                        faceImage.getEmbeddingImageUrl()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자의 얼굴 이미지 목록을 조회하는 메서드
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 얼굴 이미지 정보(id, imageUrl)가 담긴 DTO 리스트
     */
    @Transactional(readOnly = true) // 데이터를 조회만 하므로 readOnly=true 옵션으로 성능 최적화
    public List<UserFaceImageResponse> getUserFaceImages(Long userId) {
        // 1. Repository를 통해 userId에 해당하는 모든 UserFaceImage 엔티티를 조회합니다.
        List<UserFaceImage> faceImages = userFaceImageRepository.findByUserId(userId);

        // 2. 조회된 엔티티 리스트를 UserFaceImageResponseDto 리스트로 변환합니다.
        return faceImages.stream()
                .map(image -> new UserFaceImageResponse(image.getId(), image.getImageUrl()))
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
