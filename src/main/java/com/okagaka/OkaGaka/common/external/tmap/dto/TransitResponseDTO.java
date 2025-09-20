package com.okagaka.OkaGaka.common.external.tmap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransitResponseDTO(
        MetaData metaData
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MetaData(
            Plan plan
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Plan(
            java.util.List<Itinerary> itineraries
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Itinerary(
            int totalTime,       // 총 소요 시간 (초)
            int transferCount,   // 환승 횟수
            int totalWalkTime    // 총 도보 시간 (초)
    ) {}
}
//@Data
//public class TransitResponseDTO {
//    private MetaData metaData;
//
//    @Data
//    public static class MetaData {
//        private RequestParameters requestParameters;
//        private Plan plan;
//    }
//
//    @Data
//    public static class RequestParameters {
//        private String reqDttm;
//        private String startX;
//        private String startY;
//        private String endX;
//        private String endY;
//    }
//
//    @Data
//    public static class Plan {
//        private List<Itinerary> itineraries;
//    }
//
//    @Data
//    public static class Itinerary {
//        private int totalTime;
//        private int transferCount;
//        private int totalWalkDistance;
//        private int totalDistance;
//        private int totalWalkTime;
//        private Fare fare;
//        private List<Leg> legs;
//    }
//
//    @Data
//    public static class Fare{
//        private Regular regular;
//    }
//
//    @Data
//    public static class Regular {
//        private int totalFare; // type이 number?
//        private Currency currency;
//    }
//
//    @Data
//    public static class Currency {
//        private String currency;
//    }
//
//    @Data
//    public static class Leg {
//        private String mode;
//        private String route;
//        private String routeId;
//        private int service;
//        private List<Lane> lane;
//        private Point start;
//        private Point end;
//
//    }
//
//    @Data
//    public static class Lane {
//        private int service;
//        private String route;
//        private String routeId;
//    }
//
//
//    @Data
//    public static class Point {
//        private String name;
//        private double lat; // type이 number
//        private double lon; // type이 number
//    }
//
//}

