//package com.okagaka.OkaGaka.common.external.tmap;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.util.UriComponentsBuilder;
//
//import java.util.List;
//import java.util.Map;
//
//@Component
//@RequiredArgsConstructor
//@Getter
//public class TmapRouteClient {
//
//    @Value("${tmap.appKey}")
//    private String appKey;
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    public JsonNode getRouteWithViaPoints(Map<String, Object> routeRequestBody) {
//        String url = UriComponentsBuilder
//                .fromHttpUrl("https://apis.openapi.sk.com/tmap/routes/routeSequential30")
//                .queryParam("version", "1")
//                .build()
//                .toUriString();
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set("Accept", "application/json");
//        headers.set("appKey", appKey);
//
//        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(routeRequestBody, headers);
//
//        ResponseEntity<JsonNode> response = restTemplate.exchange(
//                url,
//                HttpMethod.POST,
//                requestEntity,
//                JsonNode.class
//        );
//
//        return response.getBody();
//    }
//}