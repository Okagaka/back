package com.okagaka.OkaGaka.common.external.openweathermap;

import com.okagaka.OkaGaka.common.external.openweathermap.dto.WeatherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OpenWeatherMapClient {

    private final WebClient openWeatherMapWebClient;

    @Value("${openweathermap.apiKey}")
    private String apiKey;

    public WeatherResponse getCurrentWeather(String lat, String lon) {
        String uri = UriComponentsBuilder
                .fromPath("/data/2.5/weather")
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("appid", apiKey)
                .queryParam("units", "metric") // 온도를 섭씨(Celsius)로 받음
                .queryParam("lang", "kr")      // 설명을 한국어로 받음
                .toUriString();

        return openWeatherMapWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(WeatherResponse.class)
                .block();
    }
}
