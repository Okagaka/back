package com.okagaka.OkaGaka.domain.carrequest.entity;

import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.domain.aidecision.entity.AiDecision;
import jakarta.persistence.*;
import lombok.*;
import com.okagaka.OkaGaka.common.utils.BaseTimeEntity;

@Entity
@Table(name = "car_request")
@Getter
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

    @Column(nullable = false)
    private String currentLocation;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private Integer expectedDuration; // 예상 이용 시간 (분 단위)

    @OneToOne(mappedBy = "carRequest", fetch = FetchType.LAZY)
    private AiDecision aiDecision;

    // 상태 Enum (Requested, Accepted, Rejected, Completed)
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private CarRequestStatus status;

    public enum CarRequestStatus {
        REQUESTED, ACCEPTED, REJECTED, COMPLETED
    }
}
