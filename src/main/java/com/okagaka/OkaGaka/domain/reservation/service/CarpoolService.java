package com.okagaka.OkaGaka.domain.reservation.service;

import com.okagaka.OkaGaka.common.external.tmap.Coordinate;
import com.okagaka.OkaGaka.common.external.tmap.dto.MatrixRouteInfoDTO;
import com.okagaka.OkaGaka.domain.tmap.service.TmapService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;

import com.okagaka.OkaGaka.domain.reservation.entity.Reservation;
import com.okagaka.OkaGaka.domain.reservation.dto.ReservationRequest;
import com.okagaka.OkaGaka.domain.tmap.dto.CarpoolCheckResult;
import com.okagaka.OkaGaka.common.external.tmap.TmapGeocodingClient;
import com.okagaka.OkaGaka.common.external.tmap.TmapRouteMatrixService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarpoolService {

    private final int MAX_CARPOOL_SIZE = 3;
    private final TmapService tmapService;
    private final TmapGeocodingClient tmapGeocodingClient;
    private final TmapRouteMatrixService tmapRouteMatrixService;

    public CarpoolCheckResult optimizeCarpool(List<Reservation> confirmedReservations, ReservationRequest newRequest, int newRequestSoloTravelTimeSec) {

        System.out.println("\n===== [CarpoolService] 최적 경로 탐색 시작 =====");
        System.out.println(">> 입력: 기존 확정 예약 " + confirmedReservations.size() + "건, 신규 요청 1건");

        // 새 예약도 포함

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

        // DTO의 날짜와 시간을 합쳐 LocalDateTime 생성
        LocalDateTime desiredArrivalDateTime = newRequest.getDate().atTime(newRequest.getDesiredArrivalTime());

        Reservation newReservation = Reservation.builder()
                .departureLatitude(Double.parseDouble(newDeparture.getLat()))
                .departureLongitude(Double.parseDouble(newDeparture.getLon()))
                .destinationLatitude(Double.parseDouble(newDestination.getLat()))
                .destinationLongitude(Double.parseDouble(newDestination.getLon()))
                .desiredArrivalTime(desiredArrivalDateTime)
                .build();

        List<Reservation> allReservations = new ArrayList<>(confirmedReservations);
        allReservations.add(newReservation);

        // --- 1단계: 데이터 준비 ---
        System.out.println("\n--- [단계 1] 데이터 준비: 모든 출발/도착지를 Point 객체로 변환 ---");

        // 1. 각 예약에서 dep, des 좌표 객체 생성
        List<Point> points = new ArrayList<>();
        Map<Reservation, Point> depMap = new HashMap<>();
        Map<Reservation, Point> desMap = new HashMap<>();

        for (Reservation r : allReservations) {
            Point dep = new Point(r, true, r.getDepartureLatitude(), r.getDepartureLongitude());
            Point des = new Point(r, false, r.getDestinationLatitude(), r.getDestinationLongitude());
            points.add(dep);
            points.add(des);
            depMap.put(r, dep);
            desMap.put(r, des);
        }

        System.out.println(">> 총 " + points.size() + "개의 지점(Point) 생성 완료.");
        System.out.println(points);

        // --- 2단계: 유효 경로 후보 생성 ---
        System.out.println("\n--- [단계 2] 유효 경로 생성: DFS 알고리즘으로 모든 가능한 경로 조합 탐색 ---");
        // 2. 유효 경로 후보 생성 (DFS + 백트래킹)
        List<List<Point>> allValidPaths = new ArrayList<>();
        generatePaths(new ArrayList<>(), points, depMap, desMap, allValidPaths);
        System.out.println(">> 총 " + allValidPaths.size() + "개의 유효한 경로 후보 생성 완료.");

        System.out.println("--- 생성된 유효 경로 목록 ---");
        int pathCount = 1;
        for (List<Point> path : allValidPaths) {
            System.out.println("경로 " + pathCount++ + ": " + path);
        }
        System.out.println("--------------------------");

        if (allValidPaths.isEmpty()) {
            System.out.println(">> 유효 경로가 없어 카풀 불가.");
            return null; // 카풀 불가
        }

        // 3. TMAP 경로 매트릭스 API 호출 -> durationMatrix 생성
//        int minTimeSec = Integer.MAX_VALUE;
//        List<Point> optimalPath = null;
//        for (List<Point> pathCandidate : allValidPaths) {
//            int travelTimeSec = tmapService.calculateMultiRouteTravelTime(pathCandidate);
//            if (travelTimeSec < minTimeSec) {
//                minTimeSec = travelTimeSec;
//                optimalPath = pathCandidate;
//            }
//        }

        // --- 3단계: 이동 시간표 생성 ---
        System.out.println("\n--- [단계 3] 이동 시간표 생성: TMAP 경로 매트릭스 API 호출 ---");
        // 3. TMAP 경로 매트릭스 API 호출 -> durationMatrix 생성
        List<Coordinate> coords = points.stream()
                .map(p -> new Coordinate(
                        String.valueOf(p.getLat()),
                        String.valueOf(p.getLon())
                ))
                .toList();

        List<MatrixRouteInfoDTO> routeInfos = tmapRouteMatrixService.estimateOptimizedTravelTime(coords, coords);

        // durationMatrix 구성
        final int INF = 1_000_000_000;
        int n = coords.size();
        int[][] durationMatrix = new int[n][n];
        for (int i = 0; i < n; i++) Arrays.fill(durationMatrix[i], INF);
        for (MatrixRouteInfoDTO dto : routeInfos) {
            durationMatrix[dto.getOriginIndex()][dto.getDestinationIndex()] = dto.getDuration();
        }
        System.out.println(">> 모든 지점 간 이동 시간표(durationMatrix) 생성 완료.");

        // 4. 모든 유효 경로 후보 평가
//        int minTimeSec = Integer.MAX_VALUE;
//        List<Point> optimalPath = null;


        Map<Point, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < points.size(); i++) indexMap.put(points.get(i), i);

        // --- 4단계: 최적 경로 평가 ---
        System.out.println("\n--- [단계 4] 최적 경로 평가: " + allValidPaths.size() + "개 경로의 총 소요 시간 계산 ---");
        // 4. 모든 경로 후보 평가
        long minTimeSec = Long.MAX_VALUE;
        List<Point> optimalPath = null;

        for (List<Point> pathCandidate : allValidPaths) {
            if (pathCandidate == null || pathCandidate.size() < 2) continue;
            long total = 0L;
            boolean pruned = false;

            for (int i = 0; i < pathCandidate.size() - 1; i++) {
                Integer fromIdx = indexMap.get(pathCandidate.get(i));
                Integer toIdx   = indexMap.get(pathCandidate.get(i + 1));
                if (fromIdx == null || toIdx == null) { pruned = true; break; }

                int d = durationMatrix[fromIdx][toIdx];
                if (d == INF) { pruned = true; break; }

                total += d;
                if (total >= minTimeSec) { pruned = true; break; }
            }

            if (!pruned && total < minTimeSec) {
                minTimeSec = total;
                optimalPath = pathCandidate;
            }
        }
        System.out.println(">> 최적 경로 탐색 완료!");
        System.out.println(">> 최단 소요 시간: " + minTimeSec + "초");
        System.out.println(">> 최적 경로: " + optimalPath);


        if (optimalPath == null) {
            return null;
        }

        // --- 5단계: 대략적인 시간표 계산 ---
        System.out.println("\n--- [단계 5] 대략적인 시간표 계산: 역방향 계산으로 모든 지점의 도착/출발 시간 결정 ---");
        // 5. 역방향 누적 계산으로 모든 참여자의 출발/도착 시간 계산(대략적인)
        Map<Point, LocalDateTime> roughTimetable = calculateArrivalTimes(optimalPath, durationMatrix, indexMap, desMap);
        System.out.println(">> 대략적인 시간표(arrivalTimesAtPoints) 생성 완료.");
        System.out.println(roughTimetable);

        // 이제 departureTimes 맵에는 각 예약별로 계산된 출발 시간이 들어있습니다.
        // 이 결과를 포함하여 CarpoolCheckResult 객체를 생성하고 반환하면 됩니다.
//        System.out.println("계산된 최적 경로: " + optimalPath);
//        System.out.println("계산된 출발 시간: " + arrivalTimesAtPoints);

        // --- 6단계: '최종 경로 검증' API 호출
        System.out.println("\n--- [단계 5] 최종 경로 검증: Prediction API로 더 정확한 시간 예측 ---");
        // 대략적인 시간표에서 첫 번째 출발자의 출발 시간을 가져옴
        LocalDateTime firstDepartureTime = roughTimetable.get(optimalPath.get(0));

        // TmapService의 새 메서드를 호출하여 교통 상황이 반영된 '정확한' 시간표를 받음
        Map<Point, LocalDateTime> verifiedTimetable = tmapService.getVerifiedTimetable(optimalPath, firstDepartureTime);
        System.out.println(">> 검증된 최종 운행 시간표: " + verifiedTimetable);

        // 최종 효율성 검증 로직( (A 따로 가는 시간) + (B 따로 가는 시간) < (A와 B가 함께 가는 시간) -> 비효율적이라 카풀 생성 거부해야 함)
        System.out.println("\n--- [추가 단계] 최종 경로 효율성 검증 ---");
        // 1. 각자 따로 갔을 때의 소요 시간 합 계산
        long sumOfIndividualTimes = newRequestSoloTravelTimeSec;
        for (Reservation r : confirmedReservations) {
            sumOfIndividualTimes += r.getTravelTimeSec();
        }

        // 2. 현실적인 카풀 총 소요 시간 계산
        LocalDateTime carpoolStartTime = verifiedTimetable.get(optimalPath.get(0));
        LocalDateTime carpoolEndTime = verifiedTimetable.get(optimalPath.get(optimalPath.size() - 1));
        long verifiedCarpoolTotalTime = Duration.between(carpoolStartTime, carpoolEndTime).getSeconds();

        // 3. 두 시간 비교 (10분의 여유시간 허용)
        final int EFFICIENCY_THRESHOLD_SECONDS = 600; // 10분
        System.out.println(">> 각자 이동 시 총합 (현실 예측 기반): " + sumOfIndividualTimes + "초");
        System.out.println(">> 카풀 시 총 소요시간 (현실 예측 기반): " + verifiedCarpoolTotalTime + "초");

        if (verifiedCarpoolTotalTime > sumOfIndividualTimes + EFFICIENCY_THRESHOLD_SECONDS) {
            System.out.println(">> [검증 실패] 카풀이 각자 이동하는 것보다 10분 이상 비효율적이므로 카풀을 생성하지 않습니다.");
            return null; // 카풀이 비효율적이므로 null 반환
        }
        System.out.println(">> [검증 성공] 카풀이 더 효율적이거나, 비효율이 10분 미만입니다.");



        // --- 7단계: 최종 시간 보정 (희망 도착 시간 준수)
        System.out.println("\n--- [단계 6] 최종 시간 보정: 희망 도착 시간 준수를 위해 전체 일정 조정 ---");
        Point finalDestinationPoint = optimalPath.get(optimalPath.size() - 1);
        LocalDateTime predictedFinalArrivalTime = verifiedTimetable.get(finalDestinationPoint);
        LocalDateTime desiredFinalArrivalTime = finalDestinationPoint.getReservation().getDesiredArrivalTime();

        Map<Point, LocalDateTime> adjustedTimetable = verifiedTimetable; // 기본값은 검증된 시간표
        if (predictedFinalArrivalTime.isAfter(desiredFinalArrivalTime)) {
            // 지연 시간 계산
            Duration delay = Duration.between(desiredFinalArrivalTime, predictedFinalArrivalTime);
            System.out.println(">> 예측된 도착 시간이 희망 시간보다 " + delay.toMinutes() + "분 늦어 전체 일정을 앞당깁니다.");

            // 지연 시간만큼 전체 시간표를 앞으로 당김
            adjustedTimetable = new HashMap<>();
            for (Map.Entry<Point, LocalDateTime> entry : verifiedTimetable.entrySet()) {
                adjustedTimetable.put(entry.getKey(), entry.getValue().minus(delay));
            }
            System.out.println(">> 보정된 최종 운행 시간표: " + adjustedTimetable);
        } else {
            System.out.println(">> 예측된 도착 시간이 희망 시간보다 빠르거나 같아 추가 보정 없음.");
        }

        // --- 8단계: '정확한' 시간표로 최종 결과 생성 (입력값만 변경)
        System.out.println("\n--- [단계 7] 최종 결과 패키징 ---");
        // 기존의 createCarpoolCheckResult에 '대략적인' 시간표(roughTimetable) 대신
        // '정확한' 시간표(verifiedTimetable)를 전달합니다.
        return createCarpoolCheckResult(optimalPath, newReservation, adjustedTimetable, minTimeSec);

//        // --- 6단계: 결과 패키징 ---
//        System.out.println("\n--- [단계 6] 최종 결과 패키징 ---");
//        CarpoolCheckResult result = createCarpoolCheckResult(optimalPath, newReservation, arrivalTimesAtPoints, minTimeSec);
//        System.out.println("===== [CarpoolService] 최적 경로 탐색 종료 =====");
//        return result;


    }

    /**
     * 계산 결과를 바탕으로 최종 CarpoolCheckResult 객체를 생성하는 헬퍼 메서드
     */
    private CarpoolCheckResult createCarpoolCheckResult(List<Point> optimalPath,
                                                        Reservation newReservation,
                                                        Map<Point, LocalDateTime> arrivalTimesAtPoints,
                                                        long totalTravelTime) {

        LocalDateTime newReservationDepartureTime = null;
        LocalDateTime newReservationArrivalTime = null;
        Map<Long, LocalDateTime> updatedDepartureTimes = new HashMap<>();
        Map<Long, LocalDateTime> updatedArrivalTimes = new HashMap<>();
        Map<Long, Integer> updatedTravelTimesSec = new HashMap<>();

        // 임시로 각 '기존' 예약의 출발/도착 시간을 저장할 맵
        Map<Long, LocalDateTime> tempDepartureTimes = new HashMap<>();
        Map<Long, LocalDateTime> tempArrivalTimes = new HashMap<>();

        for (Point p : optimalPath) {
            Reservation r = p.getReservation();

            // 신규 예약자인 경우 (ID가 없거나, newReservation 객체와 동일)
            // 참고: newReservation은 아직 DB에 저장 전이라 id가 null입니다.
            if (r.getId() == null) {
                if (p.isDeparture()) {
                    newReservationDepartureTime = arrivalTimesAtPoints.get(p);
                } else {
                    newReservationArrivalTime = arrivalTimesAtPoints.get(p);
                }
            }
            // 기존 예약자인 경우
            else if (p.isDeparture()) {
//                updatedDepartureTimes.put(r.getId(), arrivalTimesAtPoints.get(p));
                tempDepartureTimes.put(r.getId(), arrivalTimesAtPoints.get(p));
            } else {
                // 기존 예약자의 도착 지점 시간을 updatedArrivalTimes 맵에 저장
//                updatedArrivalTimes.put(r.getId(), arrivalTimesAtPoints.get(p));
                tempArrivalTimes.put(r.getId(), arrivalTimesAtPoints.get(p));
            }
        }

        // 임시 저장된 시간으로 기존 예약자들의 변경된 소요 시간 계산
        for (Long reservationId : tempDepartureTimes.keySet()) {
            LocalDateTime dep = tempDepartureTimes.get(reservationId);
            LocalDateTime arr = tempArrivalTimes.get(reservationId);
            if (dep != null && arr != null) {
                int travelTime = (int) Duration.between(dep, arr).getSeconds();
                updatedTravelTimesSec.put(reservationId, travelTime);
            }
        }

        // 최종 맵에 데이터 복사
        updatedDepartureTimes.putAll(tempDepartureTimes);
        updatedArrivalTimes.putAll(tempArrivalTimes);

        // 신규 예약자의 순수 이동 시간 계산
        int newReservationTravelTimeSec = (int) Duration.between(
                newReservationDepartureTime, newReservationArrivalTime
        ).getSeconds();

        return new CarpoolCheckResult(
                optimalPath,
                newReservationDepartureTime,
                newReservationArrivalTime,
                newReservationTravelTimeSec,
                updatedDepartureTimes,
                updatedArrivalTimes,
                updatedTravelTimesSec,
                totalTravelTime
        );
    }

    /**
     * 5. 역방향 누적 계산으로 출발 시간을 결정
     * @param optimalPath 최적 경로
     * @param durationMatrix 지점 간 이동 시간 행렬
     * @param indexMap Point 객체와 durationMatrix의 인덱스를 매핑
     * @param desMap Reservation과 도착지 Point를 매핑
     * @return 각 Reservation별 출발 시간을 담은 Map
     */
    private Map<Point, LocalDateTime> calculateArrivalTimes(
            List<Point> optimalPath,
            int[][] durationMatrix,
            Map<Point, Integer> indexMap,
            Map<Reservation, Point> desMap) {

        // 결과(각 지점의 도착 시간)를 저장할 Map
        Map<Point, LocalDateTime> arrivalTimesAtPoints = new HashMap<>();

        // 1. 기준 시간 설정: 경로의 가장 마지막 지점 도착 시간 설정
        Point lastPoint = optimalPath.get(optimalPath.size() - 1);
        Reservation lastReservation = lastPoint.getReservation();
        // 실제로는 Reservation 객체에 저장된 desiredArrivalTime을 사용해야 합니다.
        // 여기서는 예시로 현재 시간을 기준으로 하지만, 실제로는 아래와 같이 가져와야 합니다.
         LocalDateTime lastArrivalTime = lastReservation.getDesiredArrivalTime();
        System.out.println(">> 역방향 계산 시작. 기준 시간 (마지막 지점 " + lastPoint + ") -> " + lastArrivalTime);
        arrivalTimesAtPoints.put(lastPoint, lastArrivalTime);


        // 2. 역방향으로 순회하며 각 지점의 도착 시간 계산
        for (int i = optimalPath.size() - 2; i >= 0; i--) {
            Point currentPoint = optimalPath.get(i);
            Point nextPoint = optimalPath.get(i + 1);

            // currentPoint -> nextPoint 로의 이동 시간 (초)
            int travelDurationSec = durationMatrix[indexMap.get(currentPoint)][indexMap.get(nextPoint)];

            // 다음 지점의 도착 시간에서 이동 시간을 빼서 현재 지점의 도착 시간을 계산
            LocalDateTime currentPointArrivalTime = arrivalTimesAtPoints.get(nextPoint).minusSeconds(travelDurationSec);
            System.out.print("   - " + currentPoint + " 도착 시간 계산: " + arrivalTimesAtPoints.get(nextPoint) + " - " + travelDurationSec + "초 = " + currentPointArrivalTime);

            // 3. 희망 도착 시간 보정: 만약 현재 지점이 누군가의 '목적지'라면
            if (currentPoint.isDestination()) {
                Reservation reservation = currentPoint.getReservation();
                 LocalDateTime desired = reservation.getDesiredArrivalTime(); // <<-- 실제 희망 도착 시간
//                LocalDateTime desired = LocalDateTime.now().minusMinutes(10); // <<-- 예시용 시간

                // 계산된 도착 시간이 희망 도착 시간보다 늦다면, 희망 도착 시간으로 조정 (더 이른 시간으로)
                if (currentPointArrivalTime.isAfter(desired)) {
                    currentPointArrivalTime = desired;
                    System.out.print(" -> **시간 보정 발생!** 희망 시간(" + desired + ")으로 조정");
                }
            }
            System.out.println(); // 줄바꿈
            arrivalTimesAtPoints.put(currentPoint, currentPointArrivalTime);
        }

        // 4. 최종 결과(예약별 출발 시간) 정리
        Map<Reservation, LocalDateTime> departureTimesByReservation = new HashMap<>();
        for (Reservation reservation : desMap.keySet()) { // 모든 예약에 대해
            // 예약에 해당하는 '출발지' Point를 찾아야 합니다.
            // 이 예제에서는 depMap이 이 메서드에 없으므로, optimalPath에서 직접 찾습니다.
            for(Point p : optimalPath){
                if(p.isDeparture() && p.getReservation().equals(reservation)){
                    departureTimesByReservation.put(reservation, arrivalTimesAtPoints.get(p));
                    break;
                }
            }
        }

//        return departureTimesByReservation;
        return arrivalTimesAtPoints;
    }

    // DPS + 백트래킹
    private void generatePaths(List<Point> currentPath, List<Point> remainingPoints,
                               Map<Reservation, Point> depMap,
                               Map<Reservation, Point> desMap,
                               List<List<Point>> result) {
        if (remainingPoints.isEmpty()) {
            result.add(new ArrayList<>(currentPath));
            return;
        }

        for (Point p : new ArrayList<>(remainingPoints)) {
            if(isValidToAdd(currentPath, p, depMap, desMap)) {
                currentPath.add(p);
                List<Point> nextRemaining = new ArrayList<>(remainingPoints);
                nextRemaining.remove(p);
                generatePaths(currentPath, nextRemaining, depMap, desMap, result);
                currentPath.remove(currentPath.size() - 1);
            }
        }

    }

    private boolean isValidToAdd(List<Point> currentPath, Point p,
                                 Map<Reservation, Point> depMap, Map<Reservation, Point> desMap) {
        // p가 목적지라면, dep가 이미 currentPath에 존재해야 함
        if(!p.isDeparture()) {
            Point dep = depMap.get(p.getReservation());
            return currentPath.contains(dep);
        }
        return true;
    }



    // 예약의 출발지 또는 도착지를 하나의 객체로 추상화
    @Getter
    @ToString(of = {"reservation", "isDeparture", "lat", "lon"})
    @RequiredArgsConstructor
    public static class Point {
        private final Reservation reservation; // 해당 Point가 어떤 예약에 속하는지 연결
        private final boolean isDeparture; // 출발지/도착지 구분(true: 출발지, false:도착지)
        private final double lat;
        private final double lon;

        public boolean isDestination() {
            return !isDeparture;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Point)) return false;
            Point other = (Point) o;

            // 좌표와 출발/도착 여부를 함께 비교
            return Double.compare(lat, other.lat) == 0 &&
                    Double.compare(lon, other.lon) == 0 &&
                    isDeparture == other.isDeparture &&
                    Objects.equals(reservation.getId(), other.reservation.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(lat, lon, isDeparture,
                    reservation != null ? reservation.getId() : null);
        }

    }
}
