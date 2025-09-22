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
                .encode()
                .toUriString();
//                .build().toString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("appKey", appKey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

        JsonNode coordinateInfo = response.getBody().path("coordinateInfo");

        System.out.println("TMAP Geocoding 응답: " + response.getBody().toPrettyString());

        if (coordinateInfo.isMissingNode() || coordinateInfo.path("lon").isMissingNode()) {
            throw new RuntimeException("좌표 변환 실패: " +
                    "cityDo=" + cityDo + ", guGun=" + guGun + ", dong=" + dong + ", bunji=" + bunji);
        }

        String longitude = coordinateInfo.path("lon").asText();
        String latitude = coordinateInfo.path("lat").asText();

        System.out.println("TMAP 좌표 변환 결과: lat=" + latitude + ", lon=" + longitude);

        return new Coordinate(latitude, longitude);
    }

    /**
     * 좌표를 주소로 변환합니다. (역 지오코딩)
     * 건물명(buildingName)을 우선적으로 반환하며, 없을 경우 '도로명 + 건물번호'를 반환합니다.
     * @param longitude 경도
     * @param latitude 위도
     * @return 변환된 건물명 또는 도로명 주소
     */
    public String getAddress(double longitude, double latitude) {
        String url = UriComponentsBuilder.fromHttpUrl("https://apis.openapi.sk.com/tmap/geo/reversegeocoding")
                .queryParam("version", "1")
                .queryParam("lat", latitude)
                .queryParam("lon", longitude)
                .queryParam("addressType", "A03") // A03: 도로명 주소
                .encode()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("appKey", appKey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

        JsonNode addressInfo = response.getBody().path("addressInfo");

        if (addressInfo.isMissingNode()) {
            throw new RuntimeException("주소 변환 실패: lat=" + latitude + ", lon=" + longitude);
        }

        // 1. buildingName을 우선적으로 가져옵니다.
        String buildingName = addressInfo.path("buildingName").asText(null);

        // 2. buildingName이 존재하고 비어있지 않다면, 해당 값을 바로 반환합니다.
        if (buildingName != null && !buildingName.isEmpty()) {
            return buildingName;
        }

        // 3. buildingName이 없는 경우, 대체 정보로 '도로명 + 건물번호'를 조합하여 반환합니다.
        String roadName = addressInfo.path("roadName").asText("");
        String buildingIndex = addressInfo.path("buildingIndex").asText("");
        String fallbackAddress = (roadName + " " + buildingIndex).trim();

        if (fallbackAddress.isEmpty()) {
            throw new RuntimeException("파싱된 주소 정보가 없습니다: lat=" + latitude + ", lon=" + longitude);
        }

        return fallbackAddress;
    }
}
