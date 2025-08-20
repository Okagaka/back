package com.okagaka.OkaGaka.common.external.tmap;

import com.okagaka.OkaGaka.common.external.tmap.dto.TransitResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TmapTransitClient {

    @Value("${tmap.appKey}")
    private String appKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public TransitResponseDTO getTransitRoute(String startX, String startY, String endX, String endY) {
        String url = "https://apis.openapi.sk.com/transit/routes";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("startX", startX);
        requestBody.put("startY", startY);
        requestBody.put("endX", endX);
        requestBody.put("endY", endY);
        requestBody.put("count", 1);
        requestBody.put("lang", 0);
        requestBody.put("format", "json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes("application/json"));
        headers.set("appKey", appKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<TransitResponseDTO> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                TransitResponseDTO.class
        );

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("대중교통 경로 탐색 실패");
        }

        return response.getBody();
    }
}


//import com.fasterxml.jackson.databind.JsonNode;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Component
//@RequiredArgsConstructor
//public class TmapTransitClient {
//
//    @Value("${tmap.appKey}")
//    private String appKey;
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    public JsonNode getTransitRoute(String startX, String startY, String endX, String endY) {
//        String url = "https://apis.openapi.sk.com/transit/routes";
//
//        // 요청 바디 설정
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("startX", startX);
//        requestBody.put("startY", startY);
//        requestBody.put("endX", endX);
//        requestBody.put("endY", endY);
//        requestBody.put("count", 1);          // 최대 결과 개수
//        requestBody.put("lang", 0);           // 0: 한국어
//        requestBody.put("format", "json");    // 응답 포맷
//
//        // 요청 헤더 설정
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setAccept(MediaType.parseMediaTypes("application/json"));
//        headers.set("appKey", appKey);
//
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//        ResponseEntity<JsonNode> response = restTemplate.exchange(
//                url,
//                HttpMethod.POST,
//                entity,
//                JsonNode.class
//        );
//
//        if (response.getStatusCode() != HttpStatus.OK) {
//            throw new RuntimeException("대중교통 경로 탐색 실패");
//        }
//
//        return response.getBody();
//
//    }
//
//}
