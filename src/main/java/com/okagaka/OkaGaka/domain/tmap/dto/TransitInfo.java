package com.okagaka.OkaGaka.domain.tmap.dto;

/**
 * 대중교통 경로의 핵심 정보를 담는 데이터 클래스
 */

public record TransitInfo(
        int totalMinutes,   // 총 소요 시간 (분)
        int walkMinutes,    // 총 도보 시간 (분)
        int transferCount   // 환승 횟수
) {}