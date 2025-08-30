package com.okagaka.OkaGaka.domain.reservation.entity;

import com.okagaka.OkaGaka.domain.reservation.enums.ProposalStatus;
import com.okagaka.OkaGaka.domain.reservation.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "carpool_proposal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarpoolProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 기존 예약 (먼저 예약한 사람)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Reservation fromReservation;

    // 새로 예약하려는 요청
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Reservation toReservation;

    // 제안된 출발 시간
    @Column(nullable = false)
    private LocalDateTime proposedDepartureTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProposalStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ProposalStatus.PENDING;
        }
    }

}
