package com.okagaka.OkaGaka.domain.stt.service;

import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechToTextService {

    public String recognizeFromFile(String filePath) throws IOException {
        log.info("=== SpeechToTextService.recognizeFromFile START ===");
        log.info("Input file path: {}", filePath);

        // 환경 변수 / 시스템 프로퍼티 확인
        String envCreds = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        String propCreds = System.getProperty("GOOGLE_APPLICATION_CREDENTIALS");
        log.info("ENV GOOGLE_APPLICATION_CREDENTIALS = {}", envCreds);
        log.info("PROP GOOGLE_APPLICATION_CREDENTIALS = {}", propCreds);

        // 파일 존재 여부 확인
        if (!Files.exists(Path.of(filePath))) {
            log.error("Audio file not found at {}", filePath);
            throw new IOException("Audio file not found at " + filePath);
        }


        try (SpeechClient speechClient = SpeechClient.create()) {

            ByteString audioBytes = ByteString.copyFrom(Files.readAllBytes(Path.of(filePath)));


            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
//                    .setSampleRateHertz(16000)
                    .setLanguageCode("ko-KR")
                    .build();


            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();

            log.info("Sending recognition request...");
            RecognizeResponse response = speechClient.recognize(config, audio);

            log.info("Recognition response received. Result count: {}", response.getResultsCount());

            StringBuilder resultText = new StringBuilder();
            for (SpeechRecognitionResult result : response.getResultsList()) {
                if (result.getAlternativesCount() > 0) {
                    SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                    log.debug("Recognized text: {}", alternative.getTranscript());
                    resultText.append(alternative.getTranscript());
                }
            }


            String finalResult = resultText.toString();
            log.info("Final recognized text: {}", finalResult);
            return finalResult;

        } catch (Exception e) {
//            e.printStackTrace();
            throw new IOException("Google STT 요청 실패: " + e.getMessage(), e);
        }
    }
}

