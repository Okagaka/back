package com.okagaka.OkaGaka.domain.aidecision.service;

import com.okagaka.OkaGaka.common.external.openweathermap.OpenWeatherMapClient;
import com.okagaka.OkaGaka.common.external.openweathermap.dto.WeatherResponse;
import com.okagaka.OkaGaka.domain.aidecision.dto.WeatherInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final OpenWeatherMapClient openWeatherMapClient;

    public WeatherInfo getCurrentWeatherInfo(String lat, String lon) {
        WeatherResponse response = openWeatherMapClient.getCurrentWeather(lat, lon);

        if (response == null || response.weather() == null || response.weather().isEmpty()) {
            // 날씨 정보를 가져오지 못한 경우 기본값 또는 예외 처리
            return new WeatherInfo("날씨 정보 없음", 0.0, 0.0);
        }

        // API는 날씨 설명을 리스트로 반환하지만, 현재 날씨는 보통 첫 번째 값을 사용
        WeatherResponse.WeatherDescription weatherDesc = response.weather().get(0);
        WeatherResponse.MainInfo mainInfo = response.main();

        // 필요한 정보만 가공하여 WeatherInfo 객체로 만들어 반환
        return new WeatherInfo(
                weatherDesc.description(),
                mainInfo.temp(),
                mainInfo.feelsLike()
        );
    }
}
