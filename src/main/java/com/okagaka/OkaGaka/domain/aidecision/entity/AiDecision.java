package com.okagaka.OkaGaka.domain.aidecision.entity;


import com.okagaka.OkaGaka.domain.carrequest.entity.CarRequest;
import com.okagaka.OkaGaka.domain.reservation.entity.Reservation;
import jakarta.persistence.*;
import lombok.*;
import com.okagaka.OkaGaka.common.utils.BaseTimeEntity;

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

    @Lob
    private String reason;

    @Column(name = "estimated_time", nullable = false)
    private Integer estimatedTime;

    public enum DecisionResult {
        Approve,
        Reject,
        Suggest_Public_Transport
    }
}
