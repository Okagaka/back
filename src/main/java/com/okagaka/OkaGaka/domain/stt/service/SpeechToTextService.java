package com.okagaka.OkaGaka.domain.stt.service;

import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class SpeechToTextService {

    public String recognizeFromFile(String filePath) throws IOException {
//        System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", "C:\\Users\\solso\\eternal-petal-455604-d6-eba6d6441ac0.json");


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

            RecognizeResponse response = speechClient.recognize(config, audio);


            StringBuilder resultText = new StringBuilder();
            for (SpeechRecognitionResult result : response.getResultsList()) {
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                resultText.append(alternative.getTranscript());
            }


            return resultText.toString();

        } catch (Exception e) {
//            e.printStackTrace();
            throw new IOException("Google STT 요청 실패: " + e.getMessage(), e);
        }
    }
}

