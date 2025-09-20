package com.okagaka.OkaGaka.common.external.tmap;

import com.okagaka.OkaGaka.common.external.tmap.dto.TransitResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TmapTransitClient {

    private final WebClient tmapWebClient;

    public TransitResponseDTO getTransitRoute(String startX, String startY, String endX, String endY) {
        // 요청 본문 생성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("startX", startX);
        requestBody.put("startY", startY);
        requestBody.put("endX", endX);
        requestBody.put("endY", endY);
        requestBody.put("count", 1); // 가장 추천하는 경로 1개만 받음
        requestBody.put("lang", 0);
        requestBody.put("format", "json");

        return tmapWebClient.post()
                .uri("/transit/routes") // BaseURL 이후의 경로
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(TransitResponseDTO.class)
                .block(); // 동기 방식 실행
    }
}

//@Component
//@RequiredArgsConstructor
//public class TmapTransitClient {
//
//    @Value("${tmap.appKey}")
//    private String appKey;
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    public TransitResponseDTO getTransitRoute(String startX, String startY, String endX, String endY) {
//        String url = "https://apis.openapi.sk.com/transit/routes";
//
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("startX", startX);
//        requestBody.put("startY", startY);
//        requestBody.put("endX", endX);
//        requestBody.put("endY", endY);
//        requestBody.put("count", 1);
//        requestBody.put("lang", 0);
//        requestBody.put("format", "json");
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setAccept(MediaType.parseMediaTypes("application/json"));
//        headers.set("appKey", appKey);
//
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//        ResponseEntity<TransitResponseDTO> response = restTemplate.exchange(
//                url,
//                HttpMethod.POST,
//                entity,
//                TransitResponseDTO.class
//        );
//
//        if (response.getStatusCode() != HttpStatus.OK) {
//            throw new RuntimeException("대중교통 경로 탐색 실패");
//        }
//
//        return response.getBody();
//    }
//}
