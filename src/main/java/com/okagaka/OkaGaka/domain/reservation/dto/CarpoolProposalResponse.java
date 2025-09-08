package com.okagaka.OkaGaka.domain.reservation.dto;

import com.okagaka.OkaGaka.domain.reservation.enums.ProposalStatus;
import com.okagaka.OkaGaka.domain.reservation.enums.ReservationStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarpoolProposalResponse {

    private Long proposalId;
    private Long fromReservationId; // 기존 예약자
    private Long toReservationId;   // 요청자
    private String fromReservationTitle;
    private String toReservationUserName;
    private LocalDateTime proposedDepartureTime; // 제안되는 새로운 출발 시간

    private LocalDateTime proposedArrivalTime;   // 제안되는 새로운 도착 시간


    private ProposalStatus status;
}
