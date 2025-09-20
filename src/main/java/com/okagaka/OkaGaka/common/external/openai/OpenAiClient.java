package com.okagaka.OkaGaka.common.external.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okagaka.OkaGaka.common.external.openai.dto.OpenAiApiRequest;
import com.okagaka.OkaGaka.common.external.openai.dto.OpenAiApiResponse;
import com.okagaka.OkaGaka.domain.aidecision.dto.OpenAiDecisionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper; // JSON String을 객체로 변환하기 위해 주입

    public OpenAiDecisionResponse getDecision(String prompt) {
        // OpenAI API가 요구하는 요청 본문 형식 생성
        var request = new OpenAiApiRequest(
                "gpt-4o", // 사용할 모델
                List.of(
                        new OpenAiApiRequest.Message("system", "너는 가족 이동을 돕는 AI 비서야."),
                        new OpenAiApiRequest.Message("user", prompt)
                ),
                new OpenAiApiRequest.ResponseFormat("json_object") // 응답을 JSON 형식으로 강제
        );

        try {
            // API 호출 및 기본 응답 수신
            OpenAiApiResponse apiResponse = openAiWebClient.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenAiApiResponse.class)
                    .block();

            // 응답 내용(content)은 JSON 형식의 문자열이므로, 이를 다시 객체로 변환
            String jsonContent = apiResponse.choices().get(0).message().content();
            return objectMapper.readValue(jsonContent, OpenAiDecisionResponse.class);

        } catch (WebClientResponseException e) {
            System.err.println("!!! OpenAI API Error !!!");
            System.err.println("Status Code: " + e.getStatusCode());
            System.err.println("Response Body: " + e.getResponseBodyAsString());
            // 실제 원인(cause)도 함께 던져서 더 자세한 추적 가능
            throw new RuntimeException("OpenAI API가 에러 응답을 반환했습니다.", e);

        } catch (Exception e) {
            System.err.println("!!! OpenAI Client Error !!!");
            e.printStackTrace();
            throw new RuntimeException("OpenAI API 호출 또는 응답 파싱 중 오류가 발생했습니다.", e);
        }
    }
}
