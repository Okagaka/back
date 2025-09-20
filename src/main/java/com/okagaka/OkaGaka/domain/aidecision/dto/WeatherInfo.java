package com.okagaka.OkaGaka.domain.aidecision.dto;

public record WeatherInfo(
        String description,  // 날씨 설명 (예: "약간 흐림")
        double temperature,  // 현재 온도 (섭씨)
        double feelsLikeTemp // 체감 온도 (섭씨)
) {}
