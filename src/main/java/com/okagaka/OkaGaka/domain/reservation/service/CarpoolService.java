package com.okagaka.OkaGaka.domain.reservation.service;

import com.okagaka.OkaGaka.common.external.tmap.Coordinate;
import com.okagaka.OkaGaka.common.external.tmap.dto.MatrixRouteInfoDTO;
import com.okagaka.OkaGaka.domain.tmap.service.TmapService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.stereotype.Service;

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

//    public CarpoolCheckResult optimizeCarpool(List<Reservation> confirmedReservations, ReservationRequest newRequest) {
//
//        // 새 예약도 포함
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
//        Reservation newReservation = Reservation.builder()
//                .departureLatitude(Double.parseDouble(newDeparture.getLat()))
//                .departureLongitude(Double.parseDouble(newDeparture.getLon()))
//                .destinationLatitude(Double.parseDouble(newDestination.getLat()))
//                .destinationLongitude(Double.parseDouble(newDestination.getLon()))
//                .build();
//
//        List<Reservation> allReservations = new ArrayList<>(confirmedReservations);
//        allReservations.add(newReservation);
//
//        // 1. 각 예약에서 dep, des 좌표 객체 생성
//        List<Point> points = new ArrayList<>();
//        Map<Reservation, Point> depMap = new HashMap<>();
//        Map<Reservation, Point> desMap = new HashMap<>();
//
//        for (Reservation r : allReservations) {
//            Point dep = new Point(r, true, r.getDepartureLatitude(), r.getDepartureLongitude());
//            Point des = new Point(r, false, r.getDestinationLatitude(), r.getDestinationLongitude());
//            points.add(dep);
//            points.add(des);
//            depMap.put(r, dep);
//            desMap.put(r, des);
//        }
//
//        // 2. 유효 경로 후보 생성 (DFS + 백트래킹)
//        List<List<Point>> allValidPaths = new ArrayList<>();
//        generatePaths(new ArrayList<>(), points, depMap, desMap, allValidPaths);
//
//        if (allValidPaths.isEmpty()) {
//            return null; // 카풀 불가
//        }
//
//        // 3. TMAP 경로 매트릭스 API 호출 -> durationMatrix 생성
////        int minTimeSec = Integer.MAX_VALUE;
////        List<Point> optimalPath = null;
////        for (List<Point> pathCandidate : allValidPaths) {
////            int travelTimeSec = tmapService.calculateMultiRouteTravelTime(pathCandidate);
////            if (travelTimeSec < minTimeSec) {
////                minTimeSec = travelTimeSec;
////                optimalPath = pathCandidate;
////            }
////        }
//
//        // 3. TMAP 경로 매트릭스 API 호출 -> durationMatrix 생성
//        List<Coordinate> coords = points.stream()
//                .map(p -> new Coordinate(p.getLat(), p.getLon()))
//                .toList();
//
//        List<MatrixRouteInfoDTO> routeInfos = tmapRouteMatrixService.estimateOptimizedTravelTime(coords, coords);
//
//        // durationMatrix 구성
//        final int INF = 1_000_000_000;
//        int n = coords.size();
//        int[][] durationMatrix = new int[n][n];
//        for (int i = 0; i < n; i++) Arrays.fill(durationMatrix[i], INF);
//        for (MatrixRouteInfoDTO dto : routeInfos) {
//            durationMatrix[dto.getOriginIndex()][dto.getDestinationIndex()] = dto.getDuration();
//        }
//
//        // 4. 모든 유효 경로 후보 평가
////        int minTimeSec = Integer.MAX_VALUE;
////        List<Point> optimalPath = null;
//
//
//        Map<Point, Integer> indexMap = new HashMap<>();
//        for (int i = 0; i < points.size(); i++) indexMap.put(points.get(i), i);
//
//        // 4. 모든 경로 후보 평가
//        long minTimeSec = Long.MAX_VALUE;
//        List<Point> optimalPath = null;
//
//        for (List<Point> pathCandidate : allValidPaths) {
//            if (pathCandidate == null || pathCandidate.size() < 2) continue;
//            long total = 0L;
//            boolean pruned = false;
//
//            for (int i = 0; i < pathCandidate.size() - 1; i++) {
//                Integer fromIdx = indexMap.get(pathCandidate.get(i));
//                Integer toIdx   = indexMap.get(pathCandidate.get(i + 1));
//                if (fromIdx == null || toIdx == null) { pruned = true; break; }
//
//                int d = durationMatrix[fromIdx][toIdx];
//                if (d == INF) { pruned = true; break; }
//
//                total += d;
//                if (total >= minTimeSec) { pruned = true; break; }
//            }
//
//            if (!pruned && total < minTimeSec) {
//                minTimeSec = total;
//                optimalPath = pathCandidate;
//            }
//        }
//
//        if (optimalPath == null) {
//            return null;
//        }
//
//        // 5. 역방향 누적 계산으로 출발 시간 결정
//
//
//
//
//
//
//    }

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
