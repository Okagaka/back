package com.okagaka.OkaGaka.common.external.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class EmbeddingResultApiClient {

    private final WebClient webClient;
    private static final Logger log = LoggerFactory.getLogger(EmbeddingResultApiClient.class);
    private final ObjectMapper objectMapper;

    public EmbeddingResultApiClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl("http://43.200.125.174:8011")
                .build();
        this.objectMapper = objectMapper;
    }

    public void sendEmbeddingResult(EmbeddingResultRequest requestDto) {
        log.info("EmbeddingResult API 호출 시작: userId={}, vehicleId={}", requestDto.getUserId(), requestDto.getVehicleId());

        try {
            // 요청 본문을 JSON 문자열로 변환하여 로그에 출력
            String requestBodyJson = objectMapper.writeValueAsString(requestDto);
            log.info(">>>>>> [API Request Body] POST /v1/embedding-result: {}", requestBodyJson);

        } catch (Exception e) {
            log.error("Failed to convert request DTO to JSON: {}", e.getMessage());
        }

        webClient.post()
                .uri("/v1/embedding-result")
                .bodyValue(requestDto)
                .retrieve()
                .toBodilessEntity() // 응답 본문이 필요 없으므로 toBodilessEntity() 사용
                .subscribe( // 비동기 방식으로 요청을 보냄
                        response -> log.info("EmbeddingResult API 호출 성공: {}", response.getStatusCode()),
                        error -> log.error("EmbeddingResult API 호출 실패: {}", error.getMessage())
                );
    }
}
