package com.okagaka.OkaGaka.domain.carrequest.dto;


import com.okagaka.OkaGaka.domain.carrequest.entity.CarRequest;

import java.time.LocalDateTime;

// 차량 요청 직후 프론트엔드에 즉시 반환되는 DTO
public record CarRequestResponse (
        Long carRequestId, // AI 분석 상태를 조회할 때 사용할 ID
        String status,
        String message,
        LocalDateTime createdAt
) {
    public static CarRequestResponse from(CarRequest carRequest) {
        return new CarRequestResponse(
                carRequest.getId(),
                carRequest.getStatus().name(),
                "차량 이용 가능 여부 분석이 시작되었습니다. 완료 시 알림이 전송됩니다.",
                carRequest.getCreatedAt()
        );
    }
}
