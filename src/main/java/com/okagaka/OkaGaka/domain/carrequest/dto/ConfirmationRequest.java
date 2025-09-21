package com.okagaka.OkaGaka.domain.carrequest.dto;

public record ConfirmationRequest(
        Choice choice
) {
    public enum Choice {
        VEHICLE, // 자율주행차 선택
        PUBLIC_TRANSPORT  // 대중교통 선택
    }
}
