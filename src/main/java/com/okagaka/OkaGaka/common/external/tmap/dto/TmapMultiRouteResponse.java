package com.okagaka.OkaGaka.common.external.tmap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmapMultiRouteResponse (
        JsonNode features
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feature(Properties properties) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(Integer index, String arriveTime) {}
}
