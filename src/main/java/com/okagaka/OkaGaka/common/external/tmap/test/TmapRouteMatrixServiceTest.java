package com.okagaka.OkaGaka.common.external.tmap.test;

import com.okagaka.OkaGaka.common.external.tmap.dto.MatrixRouteInfoDTO;
import com.okagaka.OkaGaka.common.external.tmap.TmapRouteMatrixService;
import com.okagaka.OkaGaka.common.external.tmap.Coordinate;
import com.okagaka.OkaGaka.common.external.tmap.dto.MatrixRouteInfoDTO;

import java.util.List;



public class TmapRouteMatrixServiceTest {

    public static void main(String[] args) {
        // 서비스 생성
        TmapRouteMatrixService service = new TmapRouteMatrixService();
        service.setAppKey("");

        // 출발지 목록 (origins)
        List<Coordinate> origins = List.of(
                new Coordinate(String.valueOf(33.47430580), String.valueOf(126.89841063)),
                new Coordinate(String.valueOf(33.40489690), String.valueOf(126.90455058))
        );

        // 도착지 목록 (destinations)
        List<Coordinate> destinations = List.of(
                new Coordinate(String.valueOf(33.45241976), String.valueOf(126.92468664)),
                new Coordinate(String.valueOf(37.54166), String.valueOf(126.971317))
        );

        // 경로 매트릭스 API 호출
        List<MatrixRouteInfoDTO> result = service.estimateOptimizedTravelTime(origins, destinations);

        // 결과 출력
        for (MatrixRouteInfoDTO info : result) {
            System.out.println("출발지(" + info.getOrigin().getLat() + ", " + info.getOrigin().getLon() + ") → "
                    + "도착지(" + info.getDestination().getLat() + ", " + info.getDestination().getLon() + ") "
                    + "| 소요시간(sec): " + info.getDuration());
        }
    }
}
