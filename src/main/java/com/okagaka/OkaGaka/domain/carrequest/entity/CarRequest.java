package com.okagaka.OkaGaka.domain.carrequest.entity;

import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.domain.aidecision.entity.AiDecision;
import jakarta.persistence.*;
import lombok.*;
import com.okagaka.OkaGaka.common.utils.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "car_request")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CarRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자와 연관 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

//    @Column(nullable = false)
//    private String currentLocation;

//    @Column(nullable = false)
//    private String destination;

    @Column(nullable = false)
    private double requesterLongitude;

    @Column(nullable = false)
    private double requesterLatitude;

    @Column(nullable = false)
    private double destinationLongitude;

    @Column(nullable = false)
    private double destinationLatitude;

    @Column
    private Integer expectedDuration; // 예상 이용 시간 (분 단위)

    @OneToOne(mappedBy = "carRequest", fetch = FetchType.LAZY)
    private AiDecision aiDecision;

    // AI 분석 후 확정된 시간을 저장할 필드 추가
    private LocalDateTime estimatedPickupTime;
    private LocalDateTime estimatedDestinationTime;

    @Column
    private Long carpoolGroupId; // 같은 카풀 운행을 공유하는 ID

    // 상태 Enum (Requested, Accepted, Rejected, Completed)
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private CarRequestStatus status;

    public enum CarRequestStatus {
        REQUESTED,          // 요청 접수됨 (분석 중)
        ANALYSIS_COMPLETE,  // AI 분석 완료 (사용자 확인 대기)
        CONFIRMED,          // 사용자가 차량 이용을 최종 확정함
        REJECTED,           // 규칙 기반 또는 시스템 오류로 거절됨
        CANCELLED   ,       // 사용자가 대중교통을 선택하거나 요청을 취소함
        COMPLETED
    }

    /**
     * AI 분석이 완료된 후, 계산된 예상 시간들을 저장합니다.
     * 상태는 변경하지 않습니다.
     * @param pickupTime 확정된 픽업 예상 시간
     * @param destinationTime 확정된 목적지 도착 예상 시간
     */
    public void setEstimatedTimes(LocalDateTime pickupTime, LocalDateTime destinationTime) {
        this.estimatedPickupTime = pickupTime;
        this.estimatedDestinationTime = destinationTime;
    }

    /**
     * 사용자가 최종적으로 차량 이용을 '확정'했을 때 호출됩니다.
     */
    public void confirm() {
        this.status = CarRequestStatus.CONFIRMED;
    }

//    /**
//     * AI의 승인 결정을 반영하여 요청의 상태와 확정된 시간을 업데이트합니다.
//     * @param pickupTime 확정된 픽업 예상 시간
//     * @param destinationTime 확정된 목적지 도착 예상 시간
//     */
//    public void approve(LocalDateTime pickupTime, LocalDateTime destinationTime) {
//        this.status = CarRequestStatus.ACCEPTED; // Enum에 정의된 ACCEPTED 사용
//        this.estimatedPickupTime = pickupTime;
//        this.estimatedDestinationTime = destinationTime;
//    }
}
