//package com.okagaka.OkaGaka.domain.stt;
//
//import com.google.auth.oauth2.GoogleCredentials;
//import com.google.api.gax.core.FixedCredentialsProvider;
//import com.google.cloud.speech.v1.*;
//
//import com.google.protobuf.ByteString;
//
//import java.io.FileInputStream;
//import java.nio.file.Files;
//import java.nio.file.Path;
//
//public class SpeechToTextTest {
//    public static void main(String[] args) throws Exception {
//        String audioFilePath = "C:/Users/solso/Downloads/stt_test.wav";
//        String keyFilePath = "C:/Users/solso/eternal-petal-455604-d6-eba6d6441ac0.json";
//
//        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(keyFilePath));
//        SpeechSettings speechSettings = SpeechSettings.newBuilder()
//                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
//                .build();
//
//        try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {
//            byte[] data = Files.readAllBytes(Path.of(audioFilePath));
//            RecognitionConfig config = RecognitionConfig.newBuilder()
//                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
//                    .setSampleRateHertz(48000) // 48000Hz로 맞춤
//                    .setLanguageCode("ko-KR")
//                    .build();
//            RecognitionAudio audio = RecognitionAudio.newBuilder()
//                    .setContent(ByteString.copyFrom(data))
//                    .build();
//
//            RecognizeResponse response = speechClient.recognize(config, audio);
//            response.getResultsList().forEach(result -> {
//                System.out.println(result.getAlternatives(0).getTranscript());
//            });
//        }
//    }
//}
//
//
