package com.okagaka.OkaGaka.common.external.embedding;

import com.okagaka.OkaGaka.domain.signup.dto.EmbeddingRequestDto;
import com.okagaka.OkaGaka.domain.signup.dto.EmbeddingResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class EmbeddingApiClient {

    private final WebClient webClient;

    private static final Logger log = LoggerFactory.getLogger(EmbeddingApiClient.class);

    // WebClient.Builder를 주입받아 API 서버의 기본 정보를 설정합니다.
    public EmbeddingApiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://13.124.128.236:8000")
                .build();
    }

    /**
     * 얼굴 이미지 정보로부터 Embedding 정보를 요청하는 메소드
     * @param requestDto API 요청에 필요한 데이터 (userId, image 리스트)
     * @return API 응답을 파싱한 DTO 객체
     */
    public EmbeddingResponseDto getEmbeddings(EmbeddingRequestDto requestDto) {
        return webClient.post() // POST 요청
                .uri("/v1/embeddings") // 기본 URL 뒤에 붙는 상세 경로
                .contentType(MediaType.APPLICATION_JSON) // 요청 본문의 타입은 JSON
                .bodyValue(requestDto) // 요청 본문에 실어보낼 객체
                .retrieve() // 응답을 받아옴
                .onStatus(HttpStatusCode::isError, response ->
                        // 응답 본문을 문자열로 읽어옵니다.
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    // 실제 에러 로그를 자세히 출력합니다.
                                    log.error("Embedding API 호출 실패. Status: {}, Body: {}", response.statusCode(), errorBody);
                                    // 예외를 발생시켜 트랜잭션 롤백 등을 유도합니다.
                                    return Mono.error(new RuntimeException("Embedding API 호출 실패: " + errorBody));
                                })
                )
                .bodyToMono(EmbeddingResponseDto.class) // 응답 본문을 EmbeddingResponseDto 객체로 변환
                .block(); // 비동기 스트림(Mono)의 처리가 끝날 때까지 기다린 후 최종 결과를 반환 (동기 방식 처리)
    }

}
