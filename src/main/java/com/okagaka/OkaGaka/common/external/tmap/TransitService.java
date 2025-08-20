package com.okagaka.OkaGaka.common.external.tmap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.okagaka.OkaGaka.common.external.tmap.TmapTransitClient;
import com.okagaka.OkaGaka.common.external.tmap.dto.TransitResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransitService {

    @Autowired
    private TmapTransitClient tmapTransitClient;

    public void findRoute() {
        TransitResponseDTO result = tmapTransitClient.getTransitRoute(
                "127.02479803562213",
                "37.504585233865086",
                "127.03747630119366",
                "37.479103923078995"
        );

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // 들여쓰기
            String jsonString = objectMapper.writeValueAsString(result);
            System.out.println("🚍 전체 TransitResponseDTO 출력:\n" + jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

//import com.okagaka.OkaGaka.common.external.tmap.TmapTransitClient;
//import com.okagaka.OkaGaka.common.external.tmap.dto.TransitResponseDTO;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//@Service
//public class TransitService {
//
//    @Autowired
//    private TmapTransitClient tmapTransitClient;
//
//    public void findRoute() {
//        TransitResponseDTO result = tmapTransitClient.getTransitRoute(
//                "127.02479803562213",
//                "37.504585233865086",
//                "127.03747630119366",
//                "37.479103923078995"
//        );
//
//        // ✅ DTO로부터 필요한 정보 추출
//        int totalTime = result.getMetaData()
//                .getPlan()
//                .getItineraries()
//                .get(0)
//                .getTotalTime();
//
//        System.out.println("총 소요 시간 (초): " + totalTime);
//
//        // 예: 환승 횟수
//        int transferCount = result.getMetaData()
//                .getPlan()
//                .getItineraries()
//                .get(0)
//                .getTransferCount();
//
//        System.out.println("환승 횟수: " + transferCount);
//
//        // 예: 첫번째 leg의 교통수단
//        String firstLegMode = result.getMetaData()
//                .getPlan()
//                .getItineraries()
//                .get(0)
//                .getLegs()
//                .get(0)
//                .getMode();
//
//        System.out.println("첫번째 이동 수단: " + firstLegMode);
//    }
//}

//import org.springframework.stereotype.Service;
//import com.fasterxml.jackson.databind.JsonNode;
//import org.springframework.beans.factory.annotation.Autowired;
//
//
//@Service
//public class TransitService {
//
//    @Autowired
//    private TmapTransitClient tmapTransitClient;
//
//    public void findRoute() {
//        JsonNode result = tmapTransitClient.getTransitRoute(
//                "127.02479803562213", // startX
//                "37.504585233865086", // startY
//                "127.03747630119366", // endX
//                "37.479103923078995"  // endY
//        );
//
//        // ✅ 전체 JSON 응답 출력
//        System.out.println("Tmap 응답 전체 JSON:\n" + result.toPrettyString());
//
//        // 기존처럼 totalTime 추출
//        JsonNode totalTime = result
//                .path("metaData")
//                .path("plan")
//                .path("itineraries")
//                .path(0)
//                .path("totalTime");
//
//        System.out.println("총 소요 시간 (초): " + totalTime.asInt());
//    }
//}
