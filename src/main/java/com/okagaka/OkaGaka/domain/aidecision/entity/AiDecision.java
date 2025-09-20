package com.okagaka.OkaGaka.domain.aidecision.entity;


import com.okagaka.OkaGaka.domain.carrequest.entity.CarRequest;
import com.okagaka.OkaGaka.domain.reservation.entity.Reservation;
import jakarta.persistence.*;
import lombok.*;
import com.okagaka.OkaGaka.common.utils.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_decision")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AiDecision extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 예약과의 1:1 관계 (nullable)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", unique = true)
    private Reservation reservation;

    // 차량 요청과의 1:1 관계 (nullable)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_request_id", unique = true)
    private CarRequest carRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_result", length = 30, nullable = false)
    private DecisionResult decisionResult;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "estimated_pickup_time")
    private LocalDateTime estimatedPickupTime; // 차량 픽업 예상 시간

    @Column(name = "estimated_destination_time")
    private LocalDateTime estimatedDestinationTime; // 차량 목적지 도착 예상 시간

    @Column(name = "car_total_time")
    private Integer carTotalTime;                   // 차량 총 소요 시간 (분)

    @Column(name = "transit_total_time")
    private Integer transitTotalTime;               // 대중교통 총 소요 시간 (분)

//    // 차량 이용 시 총 시간
//    @Column(name = "estimated_time", nullable = false)
//    private Integer estimatedTime;

//    @Column(name = "transit_time")
//    private Integer transitTime;

    public enum DecisionResult {
        Vehicle,
        Reject,
        Public_Transport
    }
}
