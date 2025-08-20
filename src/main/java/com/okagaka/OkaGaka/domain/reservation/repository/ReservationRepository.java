package com.okagaka.OkaGaka.domain.reservation.repository;

import com.okagaka.OkaGaka.domain.reservation.enums.ReservationStatus;
import com.okagaka.OkaGaka.domain.reservation.entity.Reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public interface ReservationRepository extends JpaRepository<Reservation, Long>{
    // 특정 사용자 ID로 예약 목록 조회
    List<Reservation> findByUserId(Long userId);

    // 특정 사용자와 예약 상태로 예약 목록 조회
    List<Reservation> findByUserIdAndStatus(Long userId, ReservationStatus status);

    // 특정 ID로 예약 조회 (존재 여부 체크용)
    @NonNull
    Optional<Reservation> findById(@NonNull Long id);

//    @Query("SELECT r FROM Reservation r WHERE r.user.familyGroup.id = :familyGroupId " +
//            "AND r.date = :date " +
//            "AND ((r.departureTime <= :end AND r.arrivalTime >= :start))")
//    List<Reservation> findByFamilyGroupAndDateAndTimeOverlap(@Param("familyGroupId") Long familyGroupId,
//                                                             @Param("date") LocalDate date,
//                                                             @Param("start") LocalTime start,
//                                                             @Param("end") LocalTime end);
    @Query("SELECT r FROM Reservation r WHERE r.user.familyGroup.id = :familyGroupId " +
            "AND (r.departureDateTime <= :endDateTime AND r.arrivalDateTime >= :startDateTime)")
    List<Reservation> findOverlappingReservations(
            @Param("familyGroupId") Long familyGroupId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    // 특정 날짜 범위로 예약 조회
//    List<Reservation> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

    // 반복 예약인지 확인
//    List<Reservation> findByIsRecurring(Boolean isRecurring);

    // 예약 상태로 예약 리스트 조회
    List<Reservation> findByStatus(ReservationStatus status);
}
