package com.okagaka.OkaGaka.common.external.openweathermap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * OpenWeatherMap API의 응답 전체를 담는 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WeatherResponse(
        @JsonProperty("weather") List<WeatherDescription> weather,
        @JsonProperty("main") MainInfo main,
        @JsonProperty("name") String cityName
) {
    /**
     * 날씨 설명 ("main": "Rain", "description": "moderate rain")
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WeatherDescription(
            @JsonProperty("main") String main,
            @JsonProperty("description") String description
    ) {}

    /**
     * 주요 날씨 정보 (온도, 체감 온도, 습도 등)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MainInfo(
            @JsonProperty("temp") double temp,
            @JsonProperty("feels_like") double feelsLike,
            @JsonProperty("humidity") int humidity
    ) {}
}
