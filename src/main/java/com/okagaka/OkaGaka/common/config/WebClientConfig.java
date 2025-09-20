package com.okagaka.OkaGaka.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Value("${tmap.appKey}")
    private String appKey;

    @Bean
    public WebClient tmapWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl("https://apis.openapi.sk.com")
                .defaultHeader("appKey", appKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public WebClient openWeatherMapWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl("https://api.openweathermap.org")
                .build();
    }

    @Value("${openai.apiKey}")
    private String openaiApiKey;

    @Bean
    public WebClient openAiWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl("https://api.openai.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

}
