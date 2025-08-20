package com.okagaka.OkaGaka.common.utils;

import javax.sound.sampled.*;
import java.io.*;

public class AudioUtil {

    /**
     * WAV 파일이 stereo(2채널)면 mono(1채널)로 변환 후 새 파일 생성
     * @param inputFile 입력 wav 파일
     * @return 모노로 변환된 파일 (입력이 이미 모노면 원본 파일 리턴)
     */
    public static File convertToMonoIfStereo(File inputFile) throws IOException, UnsupportedAudioFileException {
        AudioInputStream sourceStream = AudioSystem.getAudioInputStream(inputFile);
        AudioFormat sourceFormat = sourceStream.getFormat();

        // 채널 수가 1이면 변환 불필요
        if (sourceFormat.getChannels() == 1) {
            sourceStream.close();
            return inputFile;
        }

        // 모노 형식으로 변환 (채널 1개 유지, 샘플링 레이트, 비트 깊이 등 동일하게)
        AudioFormat monoFormat = new AudioFormat(
                sourceFormat.getEncoding(),
                sourceFormat.getSampleRate(),
                sourceFormat.getSampleSizeInBits(),
                1, // 채널 1개
                sourceFormat.getFrameSize() / sourceFormat.getChannels(), // frame size 재계산
                sourceFormat.getFrameRate(),
                sourceFormat.isBigEndian()
        );

        AudioInputStream monoStream = AudioSystem.getAudioInputStream(monoFormat, sourceStream);

        // 임시 파일 생성
        File monoFile = File.createTempFile("mono_speech_", ".wav");

        // 변환된 모노 스트림을 wav 파일로 저장
        AudioSystem.write(monoStream, AudioFileFormat.Type.WAVE, monoFile);

        // 리소스 정리
        sourceStream.close();
        monoStream.close();

        return monoFile;
    }
}

