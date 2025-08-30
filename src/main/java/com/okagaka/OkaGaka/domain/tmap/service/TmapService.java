package com.okagaka.OkaGaka.domain.tmap.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.okagaka.OkaGaka.common.exception.CustomException;
import com.okagaka.OkaGaka.common.exception.ErrorCode;
import com.okagaka.OkaGaka.common.external.tmap.TmapGeocodingClient;
import com.okagaka.OkaGaka.common.external.tmap.TmapRouteMatrixService;
import com.okagaka.OkaGaka.common.external.tmap.dto.MatrixRouteInfoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.okagaka.OkaGaka.domain.reservation.entity.Reservation;
import com.okagaka.OkaGaka.domain.reservation.dto.ReservationRequest;
import com.okagaka.OkaGaka.domain.tmap.dto.CarpoolCheckResult;
import com.okagaka.OkaGaka.common.external.tmap.Coordinate;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;



@Service
@RequiredArgsConstructor
public class TmapService {

    @Value("${tmap.appKey}")
    private String appKey;

    // 아래 삭제하기
    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    private final RestTemplate restTemplate = new RestTemplate();
    private final TmapGeocodingClient tmapGeocodingClient;
    private final TmapRouteMatrixService tmapRouteMatrixService;

    // 출발 시간 계산
    public Map<String, Object> calculateDepartureTime(Coordinate departure, Coordinate destination, LocalTime predictionTime, LocalDate predictionDate) {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://apis.openapi.sk.com/tmap/routes/prediction")
                .queryParam("version", "1")
                .queryParam("totalValue", 2)
                .build()
                .toUriString();

        ZoneOffset offset = ZoneOffset.of("+09:00");
        String formatted = toIsoOffsetDateTimeString(predictionDate, predictionTime, offset);
        System.out.println(formatted);

        Map<String, Object> departureMap = Map.of(
                "name", "출발지",
                "lon", String.valueOf(departure.getLon()),
                "lat", String.valueOf(departure.getLat()),
                "depSearchFlag", "03"
        );

        Map<String, Object> destinationMap = Map.of(
                "name", "도착지",
                "lon", String.valueOf(destination.getLon()),
                "lat", String.valueOf(destination.getLat()),
                "destSearchFlag", "03"
        );

        Map<String, Object> routesInfo = new HashMap<>();
        routesInfo.put("departure", departureMap);
        routesInfo.put("destination", destinationMap);
        routesInfo.put("predictionType", "departure");
        routesInfo.put("predictionTime", formatted);
        routesInfo.put("searchOption", "00");
        routesInfo.put("tollgateCarType", "car");
        routesInfo.put("trafficInfo", "N");

        Map<String, Object> requestBody = Map.of("routesInfo", routesInfo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("appKey", appKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode body = response.getBody();

            JsonNode features = body.path("features");
            if (features.isArray() && !features.isEmpty()) { //features.size() > 0
                JsonNode properties = features.get(0).path("properties");
                String departureTimeStr = properties.path("departureTime").asText(null);
                int totalTime = properties.path("totalTime").asInt(0);
                System.out.println(departureTimeStr);

                if (departureTimeStr != null) {
//                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
//                    OffsetDateTime odt = OffsetDateTime.parse(departureTimeStr, formatter);

//                    return odt.toLocalTime();
//                    return departureTimeStr; // ex. "2013-05-19T18:31:22+0900"
                    OffsetDateTime offsetDateTime = OffsetDateTime.parse(
                            departureTimeStr,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
                    );

//                    return offsetDateTime.toLocalDateTime();
                    Map<String, Object> result = new HashMap<>();
                    result.put("departureDateTime", offsetDateTime.toLocalDateTime());
                    result.put("totalTime", totalTime);

                    return result;

                } else {
                    throw new CustomException(ErrorCode.DEPARTURE_TIME_NOT_FOUND);
                }
            } else {
                throw new CustomException(ErrorCode.DEPARTURE_TIME_NOT_FOUND);
            }
        } else {
            throw new CustomException(ErrorCode.TMAP_GUIDE_FAILED);
        }

        // 이 줄은 더 이상 필요 없지만 혹시 모를 누락 경로 방지를 위해 사용할 수도 있음
        // throw new CustomException(ErrorCode.TMAP_GUIDE_FAILED);
    }

    // ISO-8601 표준
    public String toIsoOffsetDateTimeString(LocalDate date, LocalTime time, ZoneOffset offset) {
        LocalDateTime localDateTime = LocalDateTime.of(date, time);
        OffsetDateTime offsetDateTime = OffsetDateTime.of(localDateTime, offset);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
        return offsetDateTime.format(formatter);
    }

    public boolean canCarpool(Reservation existing, ReservationRequest request) {
        // TMAP 경로 매트릭스 API를 사용해 기존 예약 + 새 예약 병합 경로 계산
        // 도착 시간 범위 안에 둘 다 도착 가능하면 true
        return true; // 실제 API 연동 필요
    }


    // 카풀 가능 여부
    // boolean canCarpool = tmapService.canCarpoolTogether(confirmedReservations, request);
    public CarpoolCheckResult canCarpoolTogether(List<Reservation> confirmedReservations, ReservationRequest newRequest) {

        // 1. 좌표 수집
        List<Coordinate> origins = new ArrayList<>();
        List<Coordinate> destinations = new ArrayList<>();

        // 기존 예약
        for (Reservation existing : confirmedReservations) {
            origins.add(new Coordinate(
                    String.valueOf(existing.getDepartureLatitude()),
                    String.valueOf(existing.getDepartureLongitude())
            ));
            destinations.add(new Coordinate(String.valueOf(
                    existing.getDestinationLatitude()),
                    String.valueOf(existing.getDestinationLongitude())
            ));
        }

        // 신규 예약
        Coordinate newDeparture = tmapGeocodingClient.getCoordinates(
                newRequest.getDepartureCityDo(),
                newRequest.getDepartureGuGun(),
                newRequest.getDepartureDong(),
                newRequest.getDepartureBunji()
        );
        Coordinate newDestination = tmapGeocodingClient.getCoordinates(
                newRequest.getDestinationCityDo(),
                newRequest.getDestinationGuGun(),
                newRequest.getDestinationDong(),
                newRequest.getDestinationBunji()
        );

        origins.add(newDeparture);
        destinations.add(newDestination);

        // 2. TMAP 경로 매트릭스 호출
        List<MatrixRouteInfoDTO> routeInfos = tmapRouteMatrixService.estimateOptimizedTravelTime(origins, destinations);

        int maxDifferenceSec = 30 * 60;
        Map<Long, LocalDateTime> updatedDepartureTimes = new HashMap<>();

        // 3.기존 예약 비교
        for (Reservation existing : confirmedReservations) {
            MatrixRouteInfoDTO match = routeInfos.stream()
                    .filter(r ->
                            Double.parseDouble(r.getOrigin().getLat()) == existing.getDepartureLatitude() &&
                                    Double.parseDouble(r.getOrigin().getLon()) == existing.getDepartureLongitude() &&
                                    Double.parseDouble(r.getDestination().getLat()) == existing.getDestinationLatitude() &&
                                    Double.parseDouble(r.getDestination().getLon()) == existing.getDestinationLongitude()
                    )
                    .findFirst()
                    .orElse(null);

            if (match == null) {
                throw new RuntimeException("해당 예약에 대한 매트릭스 결과를 찾을 수 없음");
            }

            // 기존 예약과 30분 이상 차이 나면 카풀 불가
            long travelDiff = match.getDuration() - existing.getTravelTimeSec();
            if (Math.abs(travelDiff) > maxDifferenceSec) {
                return null; // 30분 이상 차이 → 카풀 불가
            }

            // 기존 예약 출발시간 재계산
            LocalDateTime newDepartureTime = existing.getArrivalDateTime().minusSeconds(match.getDuration());
            updatedDepartureTimes.put(existing.getId(), newDepartureTime);

            // 새로 계산된 소요 시간 저장 (승인 전까지 임시)
            existing.setRecalculatedTravelTimeSec(match.getDuration());


        }

        // 4. 신규 예약도 같은 방식으로 체크
        MatrixRouteInfoDTO newMatch = routeInfos.stream()
                .filter(r ->
                        r.getOrigin().equals(newDeparture) &&
                                r.getDestination().equals(newDestination)
                )
                .findFirst()
                .orElseThrow(() -> new RuntimeException("신규 예약에 대한 매트릭스 결과를 찾을 수 없음"));

//        int newTravelTimeSec = newMatch.getDuration();
//        if (newTravelTimeSec > newRequest.getAllowedTravelTimeSec()) {
//            return false;
//        }

        return new CarpoolCheckResult(updatedDepartureTimes, newMatch.getDuration());

//        return true; // 모든 예약이 허용 시간 이내 → 카풀 가능



//        // TMAP 경로 매트릭스 API를 사용해 기존 예약 + 새 예약 병합 경로 계산
//        // 도착 시간 범위 안에 둘 다 도착 가능하면 true
//
//        // 1. 모든 예약의 출발지 / 도착지 좌표 수집
//        List<Coordinate> waypoints = new ArrayList<>();
//
//        // 기존 예약들
//        for (Reservation existing : confirmedReservations) {
//            waypoints.add(new Coordinate(
//                    String.valueOf(existing.getDepartureLatitude()),
//                    String.valueOf(existing.getDepartureLongitude())
//            ));
//            waypoints.add(new Coordinate(
//                    String.valueOf(existing.getDestinationLatitude()),
//                    String.valueOf(existing.getDestinationLongitude())
//            ));
//        }
//
//        // 신규 예약
//        Coordinate newDeparture = tmapGeocodingClient.getCoordinates(
//                newRequest.getDepartureCityDo(),
//                newRequest.getDepartureGuGun(),
//                newRequest.getDepartureDong(),
//                newRequest.getDepartureBunji()
//        );
//        Coordinate newDestination = tmapGeocodingClient.getCoordinates(
//                newRequest.getDestinationCityDo(),
//                newRequest.getDestinationGuGun(),
//                newRequest.getDestinationDong(),
//                newRequest.getDestinationBunji()
//        );
//
//        waypoints.add(newDeparture);
//        waypoints.add(newDestination);
//
//        // 카풀(모두 합친 경우) 경로 시간 계산
//        int carpoolTotalTime = tmapRouteMatrixService
//
//        // 소요 시간 계산해서 늘어난 시간 비교
//
//
//
//
//
//        return true; // 실제 API 연동 필요
    }

}
