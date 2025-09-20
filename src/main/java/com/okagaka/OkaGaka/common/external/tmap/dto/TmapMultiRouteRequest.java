package com.okagaka.OkaGaka.common.external.tmap.dto;

public record   TmapMultiRouteRequest (
    String startName, String startX, String startY,
    String endName, String endX, String endY,
    String startTime, Integer searchOption,
    java.util.List<ViaPoint> viaPoints
) {
    public record ViaPoint(
            String viaPointId, String viaPointName,
            String viaX, String viaY
    ) {}
}
