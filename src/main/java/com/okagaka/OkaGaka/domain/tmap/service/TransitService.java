package com.okagaka.OkaGaka.domain.tmap.service;

import com.okagaka.OkaGaka.common.external.tmap.TmapTransitClient;
import com.okagaka.OkaGaka.common.external.tmap.dto.TransitResponseDTO;
import com.okagaka.OkaGaka.domain.tmap.dto.TransitInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransitService {

    private final TmapTransitClient tmapTransitClient;

    /**
     * 출발지와 목적지 좌표를 받아 대중교통 경로 정보를 조회하여 반환합니다.
     * @return 경로 정보가 담긴 TransitInfo 객체. 경로가 없으면 null을 반환합니다.
     */
    public TransitInfo getTransitInfo(String startX, String startY, String endX, String endY) {
        TransitResponseDTO result = tmapTransitClient.getTransitRoute(startX, startY, endX, endY);

        // 조회된 대중교통 경로가 없는 경우 null 반환
        if (result == null || result.metaData() == null || result.metaData().plan().itineraries().isEmpty()) {
            return null;
        }

        // 가장 추천하는 첫 번째 경로 정보를 사용
        TransitResponseDTO.Itinerary itinerary = result.metaData().plan().itineraries().get(0);

        // 1. 총 소요 시간 (초 -> 분으로 변환)
        int totalMinutes = itinerary.totalTime() / 60;

        // 2. 총 도보 시간 (초 -> 분으로 변환)
        int walkMinutes = itinerary.totalWalkTime() / 60;

        // 3. 환승 횟수
        int transferCount = itinerary.transferCount();

        // 4. 조회된 정보를 TransitInfo 객체에 담아 반환
        return new TransitInfo(totalMinutes, walkMinutes, transferCount);

    }
}
