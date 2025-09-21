package com.okagaka.OkaGaka.domain.tmap.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.okagaka.OkaGaka.common.exception.CustomException;
import com.okagaka.OkaGaka.common.exception.ErrorCode;
import com.okagaka.OkaGaka.common.external.tmap.TmapGeocodingClient;
import com.okagaka.OkaGaka.common.external.tmap.TmapMultiRouteWebClient;
import com.okagaka.OkaGaka.common.external.tmap.TmapRouteMatrixService;
import com.okagaka.OkaGaka.common.external.tmap.dto.MatrixRouteInfoDTO;
import com.okagaka.OkaGaka.common.external.tmap.dto.TmapMultiRouteRequest;
import com.okagaka.OkaGaka.common.external.tmap.dto.TmapMultiRouteResponse;
import com.okagaka.OkaGaka.domain.aidecision.service.AsyncDecisionService;
import com.okagaka.OkaGaka.domain.carrequest.dto.CarpoolPoint;
import com.okagaka.OkaGaka.domain.reservation.service.CarpoolService;
import com.okagaka.OkaGaka.domain.reservation.service.CarpoolService.Point;
import com.okagaka.OkaGaka.domain.tmap.dto.ArrivalTimes;
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
    private final ObjectMapper objectMapper;
    private final TmapGeocodingClient tmapGeocodingClient;
    private final TmapRouteMatrixService tmapRouteMatrixService;
    private final TmapMultiRouteWebClient tmapMultiRouteWebClient;

    /**
     * 차량, 요청자, 목적지 위치를 받아 예상 도착 시간들을 반환합니다.
     *
     * @param vehicleLocation   현재 차량 위치 (출발지)
     * @param requesterLocation 차량 요청자 현재 위치 (경유지)
     * @param destination       최종 목적지
     * @return 요청자 위치 및 최종 목적지 도착 시간이 담긴 ArrivalTimes 객체
     */
    public ArrivalTimes getArrivalTimes(Coordinate vehicleLocation, Coordinate requesterLocation, Coordinate destination) {
        // 1. 비즈니스 데이터를 API 요청용 DTO로 변환
        TmapMultiRouteRequest request = createRouteRequest(vehicleLocation, requesterLocation, destination);

        // 2. Client에 API 호출을 위임
        TmapMultiRouteResponse response = tmapMultiRouteWebClient.getRoute(request);

        // 3.API 응답 DTO를 서비스 결과 객체로 변환하여 반환
        return parseToArrivalTimes(response);

    }

    /**
     * API 요청 DTO(TmapMultiRouteRequest)를 생성합니다.
     */
    private TmapMultiRouteRequest createRouteRequest(Coordinate start, Coordinate via, Coordinate end) {
        DateTimeFormatter tmapFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        String startTime = LocalDateTime.now().format(tmapFormatter);

        // 경유지 정보 생성
        TmapMultiRouteRequest.ViaPoint viaPoint = new TmapMultiRouteRequest.ViaPoint(
                "requester", // 경유지 ID
                "요청자위치",   // 경유지 이름
                String.valueOf(via.getLon()),
                String.valueOf(via.getLat())
        );

        // 최종 요청 DTO 생성
        return new TmapMultiRouteRequest(
                "현재차량위치", // 출발지 이름
                String.valueOf(start.getLon()),
                String.valueOf(start.getLat()),
                "목적지",      // 목적지 이름
                String.valueOf(end.getLon()),
                String.valueOf(end.getLat()),
                startTime,
                0, // 탐색 옵션 (0: 교통 최적+추천)
                Collections.singletonList(viaPoint) // 경유지는 리스트 형태로 전달
        );
    }

    /**
     * API 응답 DTO(TmapMultiRouteResponse)를 파싱하여 ArrivalTimes 객체를 생성합니다.
     */
    private ArrivalTimes parseToArrivalTimes(TmapMultiRouteResponse response) {
        if (response == null || response.features() == null || !response.features().isArray()) {
            System.err.println(">> TMAP 응답에 features 배열이 없습니다.");
            return null;
        }

        JsonNode featuresNode = response.features();
        List<JsonNode> pointFeatures = new ArrayList<>();

        // ✅ "type"이 "Point"인 feature만 리스트에 추가
        for (JsonNode feature : featuresNode) {
            if (feature.path("geometry").path("type").asText().equals("Point")) {
                pointFeatures.add(feature);
            }
        }

        // ✅ Point 타입의 feature가 3개가 맞는지 다시 확인
        if (pointFeatures.size() != 3) {
            System.err.printf(">> TMAP에서 찾은 Point 지점이 3개가 아닙니다. (찾은 개수: %d)%n", pointFeatures.size());
            return null;
        }

        try {
            // Point feature에서 시간 정보 추출
            String requesterArriveTimeStr = pointFeatures.get(1).path("properties").path("arriveTime").asText();
            String destinationArriveTimeStr = pointFeatures.get(2).path("properties").path("arriveTime").asText();

            DateTimeFormatter responseFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            LocalDateTime arrivalAtRequester = LocalDateTime.parse(requesterArriveTimeStr, responseFormatter);
            LocalDateTime arrivalAtDestination = LocalDateTime.parse(destinationArriveTimeStr, responseFormatter);

            return new ArrivalTimes(arrivalAtRequester, arrivalAtDestination);

        } catch (Exception e) {
            System.err.println(">> TMAP 응답 시간 정보 파싱 중 오류 발생");
            e.printStackTrace();
            return null;
        }
    }
//    private ArrivalTimes parseToArrivalTimes(TmapMultiRouteResponse response){
//        // 응답 유효성 검사
//        if (response == null || response.features() == null || response.features().size() != 3) {
//            // API 응답 구조: [0]출발지, [1]경유지, [2]목적지
////            throw new RuntimeException("TMAP API 응답이 올바르지 않습니다. 예상 경로 정보가 3개가 아닙니다.");
//            System.err.println(">> TMAP에서 유효한 3개 지점 경로를 찾지 못했습니다.");
//            return null;
//        }
//
//        DateTimeFormatter responseFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
//
//        // index 1: 경유지(요청자 위치)의 도착 정보
//        String requesterArriveTimeStr = response.features().get(1).properties().arriveTime();
//        LocalDateTime arrivalAtRequester = LocalDateTime.parse(requesterArriveTimeStr, responseFormatter);
//
//        // index 2: 최종 목적지의 도착 정보
//        String destinationArriveTimeStr = response.features().get(2).properties().arriveTime();
//        LocalDateTime arrivalAtDestination = LocalDateTime.parse(destinationArriveTimeStr, responseFormatter);
//
//        return new ArrivalTimes(arrivalAtRequester, arrivalAtDestination);
//    }

    // 출발 시간 계산(예약 API에 사용)
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

    /**
     * 확정된 경로와 출발 시간을 기준으로, 교통 상황을 예측하여 더 정확한 시간표를 반환합니다.
     * @param path 최종 확정된 최적 경로 (경유지 목록)
     * @param startTime 첫 번째 출발자의 출발 시간
     * @return 각 경유지(Point)별로 예측된 도착 시간이 담긴 Map
     */
    public Map<Point, LocalDateTime> getVerifiedTimetable(List<Point> path, LocalDateTime startTime) {
        if (path == null || path.size() < 2) {
            throw new IllegalArgumentException("경로는 최소 2개 이상의 지점을 포함해야 합니다.");
        }

        String url = UriComponentsBuilder
                .fromHttpUrl("https://apis.openapi.sk.com/tmap/routes/routeSequential30")
                .queryParam("version", "1")
                .build()
                .toUriString();

        // 1. 경로 및 시간 데이터 가공
        Point startPoint = path.get(0);
        Point endPoint = path.get(path.size() - 1);
        List<Point> viaPointsList = path.subList(1, path.size() - 1);

        DateTimeFormatter tmapFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        String startTimeStr = startTime.format(tmapFormatter);

        // 2. API 요청 본문(Body) 생성
        Map<String, Object> body = new HashMap<>();
        body.put("startName", "출발");
        body.put("startX", String.valueOf(startPoint.getLon()));
        body.put("startY", String.valueOf(startPoint.getLat()));
        body.put("endName", "도착");
        body.put("endX", String.valueOf(endPoint.getLon()));
        body.put("endY", String.valueOf(endPoint.getLat()));
        body.put("startTime", startTimeStr);
        body.put("searchOption", 0); // 교통 최적 + 추천

        List<Map<String, String>> viaPointsBody = new ArrayList<>();
        for (int i = 0; i < viaPointsList.size(); i++) {
            Point p = viaPointsList.get(i);
            Map<String, String> via = new HashMap<>();
            // viaPointId는 API 응답과 매칭되지 않으므로, 단순히 순서 식별용으로 사용
            via.put("viaPointId", String.format("via%02d", i + 1));
            via.put("viaPointName", "경유지" + (i + 1));
            via.put("viaX", String.valueOf(p.getLon()));
            via.put("viaY", String.valueOf(p.getLat()));
            viaPointsBody.add(via);
        }
        body.put("viaPoints", viaPointsBody);

        // 3. API 호출
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("appKey", appKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // 반환 타입을 JsonNode로 변경하여 더 안전하게 파싱
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);

        // 4. API 응답 파싱 및 결과 가공
        Map<Point, LocalDateTime> verifiedTimetable = new HashMap<>();
        JsonNode responseBody = response.getBody();

        if (responseBody != null && responseBody.has("features")) {
            JsonNode features = responseBody.get("features");
            DateTimeFormatter responseFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

            for (JsonNode feature : features) {
                JsonNode properties = feature.path("properties");
                // API 응답의 index는 출발지=0, 첫번째 경유지=1, ..., 목적지 순서
                int index = properties.path("index").asInt();
                String arriveTimeStr = properties.path("arriveTime").asText();

                if (index < path.size()) { // 경로 리스트의 범위를 벗어나지 않는지 확인
                    Point correspondingPoint = path.get(index);
                    LocalDateTime arrivalTime = LocalDateTime.parse(arriveTimeStr, responseFormatter);
                    verifiedTimetable.put(correspondingPoint, arrivalTime);
                }
            }
        } else {
            throw new RuntimeException("TMAP 다중 경유지 경로안내 API 응답이 올바르지 않습니다.");
        }

        return verifiedTimetable;
    }

    /**
     * 카풀 시나리오를 위한 다중 경유지 경로 및 시간표를 조회합니다.
     * 차량의 현재 위치를 출발점으로 하여, 주어진 경로(path)를 순서대로 경유했을 때의 시간표를 반환합니다.
     *
     * @param startCoord 차량의 현재 위치 (API 요청의 출발점)
     * @param path 최종 확정된 최적 경로 (경유지 목록)
     * @param startTime 경로 탐색 기준 시간 (차량의 현재 출발 시간)
     * @return 각 경유지(CarpoolPoint)별 예측 도착 시간이 담긴 Map
     */
    public Map<CarpoolPoint, LocalDateTime> getVerifiedCarpoolTimetable(
            Coordinate startCoord,
            List<CarpoolPoint> path,
            LocalDateTime startTime) {

        // 1. 입력값 유효성 검사
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("경유지 목록(path)은 최소 1개 이상의 지점을 포함해야 합니다.");
        }

        String url = UriComponentsBuilder
                .fromHttpUrl("https://apis.openapi.sk.com/tmap/routes/routeSequential30")
                .queryParam("version", "1")
                .build()
                .toUriString();

        // 2. 경로 및 시간 데이터 가공 (핵심 변경 부분)
        // 도착지는 경로의 마지막 지점입니다.
        CarpoolPoint endPoint = path.get(path.size() - 1);
        // 경유지 목록은 도착지를 제외한 나머지 경로입니다.
        List<CarpoolPoint> viaPointsList = path.subList(0, path.size() - 1);

        DateTimeFormatter tmapFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        String startTimeStr = startTime.format(tmapFormatter);

        // 3. API 요청 본문(Body) 생성
        Map<String, Object> body = new HashMap<>();
        // 출발지는 파라미터로 받은 차량의 현재 위치(startCoord)를 사용합니다.
        body.put("startName", "차량현재위치");
        body.put("startX", startCoord.getLon());
        body.put("startY", startCoord.getLat());

        body.put("endName", "최종목적지");
        body.put("endX", String.valueOf(endPoint.getLon()));
        body.put("endY", String.valueOf(endPoint.getLat()));

        body.put("startTime", startTimeStr);
        body.put("searchOption", 0); // 교통 최적 + 추천

        List<Map<String, String>> viaPointsBody = new ArrayList<>();
        for (int i = 0; i < viaPointsList.size(); i++) {
            CarpoolPoint p = viaPointsList.get(i);
            Map<String, String> via = new HashMap<>();
            via.put("viaPointId", "via" + (i + 1)); // 단순 식별자
            via.put("viaPointName", p.isPickup() ? "픽업" : "목적지");
            via.put("viaX", String.valueOf(p.getLon()));
            via.put("viaY", String.valueOf(p.getLat()));
            viaPointsBody.add(via);
        }
        body.put("viaPoints", viaPointsBody);

        // 4. API 호출 (기존 코드와 동일)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("appKey", appKey); // appKey는 실제 값으로 주입해야 합니다.
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);

        // 5. API 응답 파싱 및 결과 가공 (핵심 변경 부분)
        Map<CarpoolPoint, LocalDateTime> verifiedTimetable = new HashMap<>();
        JsonNode responseBody = response.getBody();

        if (responseBody != null && responseBody.has("features")) {
            JsonNode features = responseBody.get("features");
            DateTimeFormatter responseFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

            for (JsonNode feature : features) {
                JsonNode properties = feature.path("properties");
                int index = properties.path("index").asInt();
                String arriveTimeStr = properties.path("arriveTime").asText();

                // API 응답의 index와 우리 path 리스트의 index를 매핑합니다.
                // index 0: 출발지 (우리 startCoord에 해당, 도착 시간이 없으므로 무시)
                // index 1: 첫 번째 경유지 (우리 path.get(0)에 해당)
                // index 2: 두 번째 경유지 (우리 path.get(1)에 해당)
                // ...
                // 마지막 index: 최종 목적지 (우리 path.get(path.size() - 1)에 해당)
                if (index > 0 && (index - 1) < path.size()) {
                    CarpoolPoint correspondingPoint = path.get(index - 1);
                    LocalDateTime arrivalTime = LocalDateTime.parse(arriveTimeStr, responseFormatter);
                    verifiedTimetable.put(correspondingPoint, arrivalTime);
                }
            }
        } else {
            // TMAP API 호출 실패 또는 응답 형식 오류에 대한 예외 처리
            throw new RuntimeException("TMAP 다중 경유지 경로안내 API 응답이 올바르지 않거나 호출에 실패했습니다.");
        }

        return verifiedTimetable;
    }

//    public RouteResult calculateMultiRouteTravelTime(List<CarpoolService.Point> path, LocalDateTime targetArrivalTime) throws Exception {
//
//        if (path.size() < 2) {
//            throw new IllegalArgumentException("경로는 최소 출발지와 도착지를 포함해야 합니다.");
//        }
//
//        String url = UriComponentsBuilder
//                .fromHttpUrl("https://apis.openapi.sk.com/tmap/routes/routeSequential30")
//                .queryParam("version", "1")
//                .build()
//                .toUriString();
//
//        // 출발, 목적지, 경유지 분리
//        CarpoolService.Point start = path.get(0);
//        CarpoolService.Point end = path.get(path.size() - 1);
//        List<CarpoolService.Point> vias = path.subList(1, path.size() - 1);
//
//        // 임의 출발 시간 설정(TMAP API는 출발 시간을 기준으로 계산)
//        LocalDateTime provisionalStartTime = LocalDateTime.now().plusMinutes(1); // 현재 시각 기준 임시 출발 시간
//        DateTimeFormatter startTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm"); // 티맵 API 요구사항에 맞게 수정
//        String startTimeStr = provisionalStartTime.format(startTimeFormatter);
//
//        // 요청 바디 구성
//        Map<String, Object> body = new HashMap<>();
//        body.put("reqCoordType", "WGS84GEO");
//        body.put("resCoordType", "WGS84GEO");
//        body.put("carType", 1);
//        body.put("searchOption", 0); // 교통 최적 + 추천
//
//        body.put("startName", "출발");
//        body.put("startX", String.valueOf(start.getLon()));
//        body.put("startY", String.valueOf(start.getLat()));
//
//        body.put("endName", "도착");
//        body.put("endX", String.valueOf(end.getLon()));
//        body.put("endY", String.valueOf(end.getLat()));
//
//        body.put("startTime", startTimeStr);
//
//        // viaPoints 설정
//        List<Map<String, String>> viaPoints = new ArrayList<>();
//        for (int i = 0; i < vias.size(); i++) {
//            CarpoolService.Point vp = vias.get(i);
//            Map<String, String> v = new HashMap<>();
//            v.put("viaPointId", String.format("%02d", i + 1));
//            v.put("viaPointName", vp.toString());
//            v.put("viaX", String.valueOf(vp.getLon()));
//            v.put("viaY", String.valueOf(vp.getLat()));
//            viaPoints.add(v);
//        }
//
//        body.put("viaPoints", viaPoints);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
//        headers.set("appKey", appKey);
//
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
//
//        ResponseEntity<JsonNode> response = restTemplate.exchange(
//                url,
//                HttpMethod.POST,
//                entity,
//                JsonNode.class
//        );
//
//        JsonNode responseBody = response.getBody();
//        if (responseBody == null) {
//            throw new RuntimeException("Tmap API 응답이 null입니다.");
//        }
//
//        // totalTime 추출 (초 단위)
//        // .path()를 사용하면 null 체크 없이 안전하게 접근 가능
//                JsonNode properties = responseBody.path("properties");
//                int totalTime = properties.path("totalTime").asInt();
//
//        // features -> 각 경유지 도착 시간
//                JsonNode features = responseBody.path("features");
//                Map<String, LocalDateTime> arrivalTimes = new HashMap<>();
//                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
//
//                LocalDateTime apiStartTime = null;
//
//        // for-each 구문으로 더 간결하게 순회
//                for (JsonNode feature : features) {
//                    JsonNode prop = feature.path("properties");
//                    String viaPointName = prop.path("viaPointName").asText(null); // null일 경우를 대비해 기본값 지정
//                    String arriveTimeStr = prop.path("arriveTime").asText(null);
//
//                    if (arriveTimeStr != null && viaPointName != null) {
//                        LocalDateTime time = LocalDateTime.parse(arriveTimeStr, formatter);
//                        arrivalTimes.put(viaPointName, time);
//
//                        // 출발지 도착 시간 확인
//                        // ✅ Point 객체의 toString() 결과와 비교
//                        if (viaPointName.equals(start.toString())) {
//                            apiStartTime = time;
//                        }
//                    }
//                }
//
//                if (apiStartTime == null) {
//                    throw new RuntimeException("API 응답에서 출발지에 해당하는 도착 시간을 찾을 수 없습니다.");
//                }
//
//                return new RouteResult(totalTime, arrivalTimes);
//
//    }

    public static class RouteResult {
        private final int totalTimeSec;
        private final Map<String, LocalDateTime> arrivalTimes;

        public RouteResult(int totalTimeSec, Map<String, LocalDateTime> arrivalTimes) {
            this.totalTimeSec = totalTimeSec;
            this.arrivalTimes = arrivalTimes;
        }

        public int getTotalTimeSec() {
            return totalTimeSec;
        }

        public Map<String, LocalDateTime> getArrivalTimes() {
            return arrivalTimes;
        }
    }


    // 카풀 가능 여부
    // boolean canCarpool = tmapService.canCarpoolTogether(confirmedReservations, request);
//    public CarpoolCheckResult canCarpoolTogether(List<Reservation> confirmedReservations, ReservationRequest newRequest) {
//
//        // 1. 좌표 수집
//        List<Coordinate> origins = new ArrayList<>();
//        List<Coordinate> destinations = new ArrayList<>();
//
//        // 기존 예약
//        for (Reservation existing : confirmedReservations) {
//            origins.add(new Coordinate(
//                    String.valueOf(existing.getDepartureLatitude()),
//                    String.valueOf(existing.getDepartureLongitude())
//            ));
//            destinations.add(new Coordinate(String.valueOf(
//                    existing.getDestinationLatitude()),
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
//        origins.add(newDeparture);
//        destinations.add(newDestination);
//
//        // 2. TMAP 경로 매트릭스 호출
//        List<MatrixRouteInfoDTO> routeInfos = tmapRouteMatrixService.estimateOptimizedTravelTime(origins, destinations);
//
//        int maxDifferenceSec = 30 * 60;
//        Map<Long, LocalDateTime> updatedDepartureTimes = new HashMap<>();
//
//        // 3.기존 예약 비교
//        for (Reservation existing : confirmedReservations) {
//            MatrixRouteInfoDTO match = routeInfos.stream()
//                    .filter(r ->
//                            Double.parseDouble(r.getOrigin().getLat()) == existing.getDepartureLatitude() &&
//                                    Double.parseDouble(r.getOrigin().getLon()) == existing.getDepartureLongitude() &&
//                                    Double.parseDouble(r.getDestination().getLat()) == existing.getDestinationLatitude() &&
//                                    Double.parseDouble(r.getDestination().getLon()) == existing.getDestinationLongitude()
//                    )
//                    .findFirst()
//                    .orElse(null);
//
//            if (match == null) {
//                throw new RuntimeException("해당 예약에 대한 매트릭스 결과를 찾을 수 없음");
//            }
//
//            // 기존 예약과 30분 이상 차이 나면 카풀 불가
//            long travelDiff = match.getDuration() - existing.getTravelTimeSec();
//            if (Math.abs(travelDiff) > maxDifferenceSec) {
//                return null; // 30분 이상 차이 → 카풀 불가
//            }
//
//            // 기존 예약 출발시간 재계산
//            LocalDateTime newDepartureTime = existing.getArrivalDateTime().minusSeconds(match.getDuration());
//            updatedDepartureTimes.put(existing.getId(), newDepartureTime);
//
//            // 새로 계산된 소요 시간 저장 (승인 전까지 임시)
//            existing.setRecalculatedTravelTimeSec(match.getDuration());
//
//
//        }
//
//        // 4. 신규 예약도 같은 방식으로 체크
//        MatrixRouteInfoDTO newMatch = routeInfos.stream()
//                .filter(r ->
//                        r.getOrigin().equals(newDeparture) &&
//                                r.getDestination().equals(newDestination)
//                )
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("신규 예약에 대한 매트릭스 결과를 찾을 수 없음"));
//
////        int newTravelTimeSec = newMatch.getDuration();
////        if (newTravelTimeSec > newRequest.getAllowedTravelTimeSec()) {
////            return false;
////        }
//
//        return new CarpoolCheckResult(updatedDepartureTimes, newMatch.getDuration());

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
//    }

}
