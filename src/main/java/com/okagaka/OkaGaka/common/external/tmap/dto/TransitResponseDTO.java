package com.okagaka.OkaGaka.common.external.tmap.dto;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Data
public class TransitResponseDTO {
    private MetaData metaData;

    @Data
    public static class MetaData {
        private RequestParameters requestParameters;
        private Plan plan;
    }

    @Data
    public static class RequestParameters {
        private String reqDttm;
        private String startX;
        private String startY;
        private String endX;
        private String endY;
    }

    @Data
    public static class Plan {
        private List<Itinerary> itineraries;
    }

    @Data
    public static class Itinerary {
        private int totalTime;
        private int transferCount;
        private int totalWalkDistance;
        private int totalDistance;
        private int totalWalkTime;
        private Fare fare;
        private List<Leg> legs;
    }

    @Data
    public static class Fare{
        private Regular regular;
    }

    @Data
    public static class Regular {
        private int totalFare; // type이 number?
        private Currency currency;
    }

    @Data
    public static class Currency {
        private String currency;
    }

    @Data
    public static class Leg {
        private String mode;
        private String route;
        private String routeId;
        private int service;
        private List<Lane> lane;
        private Point start;
        private Point end;

    }

    @Data
    public static class Lane {
        private int service;
        private String route;
        private String routeId;
    }


    @Data
    public static class Point {
        private String name;
        private double lat; // type이 number
        private double lon; // type이 number
    }


//    private Plan plan;
//    private RequestParameters requestParameters;
//
//    @Data
//    public static class MetaData {
//        // metaData 안에 실제 필요한 필드가 있으면 추가
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
//    public static class Fare {
//        private Regular regular;
//    }
//
//    @Data
//    public static class Regular {
//        private Currency currency;
//        private int totalFare;
//    }
//
//    @Data
//    public static class Currency {
//        private String currency; // ex) "KRW"
//    }
//
//    @Data
//    public static class Leg {
//        private String mode;
//        private String route;
//        private String routeId;
//        private String service;
//        private Lane lane;
//        private Point start;
//        private Point end;
//    }
//
//    @Data
//    public static class Lane {
//        private String service;
//        private String route;
//        private String routeId;
//    }
//
//    @Data
//    public static class Point {
//        private String name;
//        private double lat;
//        private double lon;
//    }
//
//    @Data
//    public static class RequestParameters {
//        private String reqDttm;
//        private double startX;
//        private double startY;
//        private double endX;
//        private double endY;
//    }
}

