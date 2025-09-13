package com.okagaka.OkaGaka.domain.reservation.entity;


import com.okagaka.OkaGaka.domain.reservation.enums.ReservationStatus;
import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.domain.aidecision.entity.AiDecision;
import jakarta.persistence.*;
import lombok.*;
import com.okagaka.OkaGaka.common.utils.BaseTimeEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reservation")
@Getter
@Setter
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

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "carpool_group_id")
//    private CarpoolGroup carpoolGroup;

    @Column(nullable = false)
    private String title;

//    private LocalDate startDate;
//
//    private LocalDate endDate;

//    @Column
//    private LocalDate date;

    // 출발 시각
//    @Column
//    private LocalTime departureTime;

    @Column
    private LocalDateTime departureDateTime;

    // 도착 예정 시간
//    @Column
//    private LocalTime arrivalTime;

    @Column
    private LocalDateTime arrivalDateTime;

    @Column
    private LocalDateTime desiredArrivalTime; // 희망 도착 시간

    // 출발지
//    @Column
//    private String departure;
    @Column
    private double departureLatitude;

    @Column
    private double departureLongitude;

    // 목적지
//    @Column
//    private String destination; // 추후 Zone과 연관관계로 변경 가능

    @Column
    private double destinationLatitude;

    @Column
    private double destinationLongitude;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReservationStatus status;

    private int travelTimeSec; // 경로 소요 시간(초 단위)

    private int recalculatedTravelTimeSec; // 카풀 시 재계산한 시간

//    private Boolean isRecurring;
//
//    @Column
//    private String recurrencePattern;

    @OneToOne(mappedBy = "reservation", fetch = FetchType.LAZY)
    private AiDecision aiDecision;

}
