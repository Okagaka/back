package com.okagaka.OkaGaka.domain.reservation.entity;

import com.okagaka.OkaGaka.domain.familygroup.entity.FamilyGroup;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "carpool_group")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarpoolGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 가족 그룹 (한 차량 단위)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_group_id", nullable = false)
    private FamilyGroup familyGroup;

    // 예약 목록 (양방향)
    @Builder.Default
    @OneToMany(mappedBy = "carpoolGroup", cascade = CascadeType.ALL)
    private List<Reservation> reservations = new ArrayList<>();

    // 카풀 그룹 상태 (전체 상태 관리용)
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private CarpoolGroupStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = CarpoolGroupStatus.PENDING;
        }
    }

    public enum CarpoolGroupStatus {
        PENDING,    // 승인이 필요한 상태
        CONFIRMED,  // 모두 승인 완료 상태
        REJECTED    // 거절 상태
    }


}
