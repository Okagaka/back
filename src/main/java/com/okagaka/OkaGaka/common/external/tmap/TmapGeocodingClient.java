package com.okagaka.OkaGaka.common.external.tmap;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.okagaka.OkaGaka.common.external.tmap.Coordinate;

@Component
@RequiredArgsConstructor
public class TmapGeocodingClient {
    @Value("${tmap.appKey}")
    private String appKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Coordinate getCoordinates(String cityDo, String guGun, String dong, String bunji) {
        String url = UriComponentsBuilder.fromHttpUrl("https://apis.openapi.sk.com/tmap/geo/geocoding")
                .queryParam("version", "1")
                .queryParam("city_do", cityDo)
                .queryParam("gu_gun", guGun)
                .queryParam("dong", dong)
                .queryParam("bunji", bunji)
                .queryParam("addressFlag", "F00")
                .queryParam("coordType", "WGS84GEO")
                .build().toString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("appKey", appKey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

        JsonNode coordinateInfo = response.getBody().path("coordinateInfo");

        if (coordinateInfo.isMissingNode() || coordinateInfo.path("lon").isMissingNode()) {
            throw new RuntimeException("좌표 변환 실패");
        }

        String longitude = coordinateInfo.path("lon").asText();
        String latitude = coordinateInfo.path("lat").asText();

        return new Coordinate(latitude, longitude);
    }
}
