package com.okagaka.OkaGaka.domain.user.service;

import com.okagaka.OkaGaka.common.external.embedding.EmbeddingApiClient;
import com.okagaka.OkaGaka.domain.signup.dto.EmbeddingItem;
import com.okagaka.OkaGaka.domain.signup.dto.EmbeddingRequestDto;
import com.okagaka.OkaGaka.domain.signup.dto.EmbeddingResponseDto;
import com.okagaka.OkaGaka.domain.signup.dto.FaceImageInfo;
import com.okagaka.OkaGaka.domain.user.entity.UserFaceImage;
import com.okagaka.OkaGaka.domain.user.repository.UserFaceImageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.converters.SchemaPropertyDeprecatingConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FaceEmbeddingService {

    private final UserFaceImageRepository userFaceImageRepository;
    private final EmbeddingApiClient embeddingApiClient;
    private static final Logger log = LoggerFactory.getLogger(FaceEmbeddingService.class);

    @Async // 해당 메소드는 별도의 스레드에서 비동기적으로 실행됨
    @Transactional
    public void createAndSaveEmbeddings(Long userId, List<FaceImageInfo> faceImageInfosForApi) {
        log.info("비동기 임베딩 작업 시작 (UserId: {})", userId);
        try {

            // 1. Embedding API 호출
            EmbeddingRequestDto request = new EmbeddingRequestDto(userId, faceImageInfosForApi);
            EmbeddingResponseDto response = embeddingApiClient.getEmbeddings(request);

            // 2. 결과로 embeddingUrl 업데이트
            Map<Long, String> embeddingUrlMap = response.getData().getItems().stream()
                    .collect(Collectors.toMap(EmbeddingItem::getId, EmbeddingItem::getEmbeddingUrl));

            List<UserFaceImage> imagesToUpdate = userFaceImageRepository.findAllById(embeddingUrlMap.keySet());
            imagesToUpdate.forEach(image -> {
                String embeddingUrl = embeddingUrlMap.get(image.getId());
                image.updateEmbeddingImageUrl(embeddingUrl);
            });
            log.info("비동기 임베딩 작업 완료 (UserId: {})", userId);
        } catch (Exception e) {
            log.error("비동기 임베딩 작업 중 에러 발생 (UserId: {})", userId, e);
        }

    }

}
