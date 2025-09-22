package com.okagaka.OkaGaka.domain.carrequest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.okagaka.OkaGaka.domain.carrequest.entity.CarRequest;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CarRequestListResponse {

    private Long carRequestId;
//    private double destinationLongitude;
//    private double destinationLatitude;
    private String destinationAddress;
    private LocalDateTime requestTime; // 요청 시간
    private CarRequest.CarRequestStatus status;

    // CONFIRMED 상태일 때만 포함될 필드
    private LocalDateTime estimatedPickupTime;
    private LocalDateTime estimatedDestinationTime;

    /**
     * CarRequest 엔티티를 DTO로 변환하는 정적 팩토리 메서드
     */
//    public static CarRequestListResponse from(CarRequest carRequest) {
//        CarRequestListResponseBuilder builder = CarRequestListResponse.builder()
//                .carRequestId(carRequest.getId())
//                .destinationLongitude(carRequest.getDestinationLongitude())
//                .destinationLatitude(carRequest.getDestinationLatitude())
//                .requestTime(carRequest.getCreatedAt()) // BaseTimeEntity의 생성 시간 활용
//                .status(carRequest.getStatus());
//
//        // 상태가 CONFIRMED일 경우에만 예상 시간 정보를 추가
//        if (carRequest.getStatus() == CarRequest.CarRequestStatus.CONFIRMED) {
//            builder.estimatedPickupTime(carRequest.getEstimatedPickupTime())
//                    .estimatedDestinationTime(carRequest.getEstimatedDestinationTime());
//        }
//
//        return builder.build();
//    }
}
