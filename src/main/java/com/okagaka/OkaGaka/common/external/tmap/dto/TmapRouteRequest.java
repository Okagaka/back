package com.okagaka.OkaGaka.common.external.tmap.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TmapRouteRequest {

    private String startName;
    private String startX;
    private String startY;
    private String startTime;

    private String endName;
    private String endX;
    private String endY;

    private List<ViaPoint> viaPoints;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ViaPoint {
        private String viaPointId;
        private String viaPointName;
        private String viaX;
        private String viaY;
        private String viaDetailAddress;
        private String viaPoiId;
        private String viaTime;
        private String wishStartTime;
        private String wishEndTime;
    }
}
