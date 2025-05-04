package com.okagaka.OkaGaka.domain.reservation.entity;


import com.okagaka.OkaGaka.domain.reservation.enums.ReservationStatus;
import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.domain.aidecision.entity.AiDecision;
import jakarta.persistence.*;
import lombok.*;
import com.okagaka.OkaGaka.common.utils.BaseTimeEntity;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Reservation extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자와 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalTime startTime;

    private LocalTime endTime;

    @Column
    private String startLocation;

    @Column
    private String endLocation; // 추후 Zone과 연관관계로 변경 가능

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReservationStatus status;

    private Boolean isRecurring;

    @Column
    private String recurrencePattern;

    @OneToOne(mappedBy = "reservation", fetch = FetchType.LAZY)
    private AiDecision aiDecision;


}
