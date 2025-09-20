package com.okagaka.OkaGaka.common.external.tmap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okagaka.OkaGaka.common.external.tmap.dto.TmapMultiRouteRequest;
import com.okagaka.OkaGaka.common.external.tmap.dto.TmapMultiRouteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;

@Component
@RequiredArgsConstructor
public class TmapMultiRouteWebClient {

    private final WebClient tmapWebClient;
    private final ObjectMapper objectMapper;

    public TmapMultiRouteResponse getRoute(TmapMultiRouteRequest request) {
        try {
            String responseBody = tmapWebClient.post()
                    .uri("/tmap/routes/routeSequential30?version=1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class) // ✅ 먼저 String으로 받음
                    .block();

            // ✅ 실제 응답 내용을 로그로 출력
            System.out.println(">> [TMAP Raw Response]: " + responseBody);

            // ✅ String을 DTO 객체로 변환
            return objectMapper.readValue(responseBody, TmapMultiRouteResponse.class);

        } catch (Exception e) {
            throw new RuntimeException("TMAP 응답 파싱 중 오류 발생", e);
        }
    }


//    public TmapMultiRouteResponse getRoute(TmapMultiRouteRequest request) {
//        return tmapWebClient.post()
//                .uri("/tmap/routes/routeSequential30?version=1")
//                .contentType(MediaType.APPLICATION_JSON) // POST 요청 시 Content-Type 지정
//                .bodyValue(request)
//                .retrieve()
//                .bodyToMono(TmapMultiRouteResponse.class)
//                .block(); // 동기 방식 유지를 위해 block() 사용
//    }


}
