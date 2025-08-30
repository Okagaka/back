package com.okagaka.OkaGaka.domain.reservation.repository;

import com.okagaka.OkaGaka.domain.reservation.entity.CarpoolProposal;
import com.okagaka.OkaGaka.domain.reservation.enums.ReservationStatus;
import com.okagaka.OkaGaka.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarpoolProposalRepository extends JpaRepository<CarpoolProposal, Long> {

    // 특정 예약에 대한 모든 카풀 제안 조회
//    List<CarpoolProposal> findByReservationId(Long reservationId);

    // 특정 예약 + 사용자에 대한 제안 조회
//    Optional<CarpoolProposal> findByReservationIdAndUserId(Long reservationId, Long userId);

    // 특정 사용자에게 보내진 제안 중 특정 상태의 제안들
//    List<CarpoolProposal> findByUserIdAndStatus(Long userId, ReservationStatus status);

    // 특정 예약에서 ACCEPTED된 제안이 있는지
    boolean existsByFromReservation_IdAndStatus(Long reservationId, ReservationStatus status);

    // 특정 요청자 Reservation과 상태가 PENDING인 제안 조회
    List<CarpoolProposal> findByToReservationAndStatus(Reservation toReservation, ReservationStatus status);

    // 특정 기존 예약자 Reservation이 받은 제안 조회
    List<CarpoolProposal> findByFromReservationAndStatus(Reservation fromReservation, ReservationStatus status);

    // 제안 상태가 확정된 것만 조회
//    List<CarpoolProposal> findByReservationIdAndStatusIn(Long reservationId, List<ReservationStatus> statuses);


}
