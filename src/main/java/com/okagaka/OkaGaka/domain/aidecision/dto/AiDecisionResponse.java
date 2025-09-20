package com.okagaka.OkaGaka.domain.aidecision.dto;

import com.okagaka.OkaGaka.domain.aidecision.entity.AiDecision;
import com.okagaka.OkaGaka.domain.carrequest.entity.CarRequest;

import java.time.LocalDateTime;

// AI 분석 완료 후 프론트엔드가 조회할 최종 결과 DTO
public record AiDecisionResponse(
        Long carRequestId,
        String decision, // Approve, Reject, Suggest_Public_Transport
        String reason,
        LocalDateTime pickupTime,
        LocalDateTime destinationTime,
        Integer carTotalTime,
        Integer transitTotalTime

) {

    public static AiDecisionResponse from(AiDecision aiDecision) {
        return new AiDecisionResponse(
                aiDecision.getCarRequest().getId(),
                aiDecision.getDecisionResult().name(),
                aiDecision.getReason(),
                aiDecision.getEstimatedPickupTime(),      // AiDecision에서 정보 조회
                aiDecision.getEstimatedDestinationTime(), // AiDecision에서 정보 조회
                aiDecision.getCarTotalTime(),             // AiDecision에서 정보 조회
                aiDecision.getTransitTotalTime()          // AiDecision에서 정보 조회
        );
    }
}
