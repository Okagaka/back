package com.okagaka.OkaGaka.common.external.tmap;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TmapMatrixClient {

    @Value("${tmap.appKey}")
    private String appKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public JsonNode getMatrix(List<Coordinate> origins, List<Coordinate> destinations) {
        String url = UriComponentsBuilder.fromHttpUrl("https://apis.openapi.sk.com/tmap/matrix")
                .queryParam("version", "1")
                .build().toString();

        // 요청 본문 생성
        Map<String, Object> body = new HashMap<>();
        body.put("origins", origins);
        body.put("destinations", destinations);
        body.put("transportMode", "car");
        body.put("metric", "Recommendation");

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("appKey", appKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // 요청 및 응답 처리
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            throw new RuntimeException("경로 매트릭스 조회 실패: " + response.getStatusCode());
        }
    }
}