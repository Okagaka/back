package com.okagaka.OkaGaka.domain.reservation.repository;

import com.okagaka.OkaGaka.domain.reservation.entity.CarpoolProposal;
import com.okagaka.OkaGaka.domain.reservation.enums.ProposalStatus;
import com.okagaka.OkaGaka.domain.reservation.enums.ReservationStatus;
import com.okagaka.OkaGaka.domain.reservation.entity.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CarpoolProposalRepository extends JpaRepository<CarpoolProposal, Long> {


    // 특정 예약에서 ACCEPTED된 제안이 있는지
    boolean existsByFromReservation_IdAndStatus(Long reservationId, ReservationStatus status);

    // 특정 요청자 Reservation과 상태가 PENDING인 제안 조회
    List<CarpoolProposal> findByToReservationAndStatus(Reservation toReservation, ReservationStatus status);

    // 특정 기존 예약자 Reservation이 받은 제안 조회
    List<CarpoolProposal> findByFromReservationAndStatus(Reservation fromReservation, ReservationStatus status);

    // 특정 기존 예약자 Reservation 여러 건 조회 + 상태
    List<CarpoolProposal> findByFromReservationInAndStatus(List<Reservation> fromReservations, ReservationStatus status);

    // toReservation 필드로 CarpoolProposal 목록 조회
    List<CarpoolProposal> findByToReservation(Reservation toReservation);

    @Query("""
       SELECT c 
       FROM CarpoolProposal c
       JOIN FETCH c.fromReservation r
       JOIN FETCH c.toReservation t
       WHERE c.status = :status
       AND r.user.id = :userId
       """)
    List<CarpoolProposal> findPendingProposalsForUser(@Param("userId") Long userId,
                                                      @Param("status") ProposalStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE) // 조회 시점에 DB 로우(Row)에 쓰기 잠금
    @Query("SELECT p FROM CarpoolProposal p WHERE p.id = :proposalId")
    Optional<CarpoolProposal> findByIdWithLock(@Param("proposalId") Long proposalId);


}
