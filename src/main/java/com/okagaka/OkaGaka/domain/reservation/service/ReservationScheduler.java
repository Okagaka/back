package com.okagaka.OkaGaka.domain.reservation.service;


import com.okagaka.OkaGaka.domain.reservation.entity.Reservation;
import com.okagaka.OkaGaka.domain.reservation.enums.ReservationStatus;
import com.okagaka.OkaGaka.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Component
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationRepository reservationRepository;

    // 예상 도착 시간보다 10분 늦는 경우까지 여유를 둡니다.
    private static final int GRACE_PERIOD_MINUTES = 10;

    /**
     * 5분마다 한 번씩 실행되며, 완료된 예약 상태를 업데이트합니다.
     */
    @Scheduled(fixedRate = 300000) // 5분 = 300,000 밀리초
    @Transactional
    public void updateCompletedReservations() {
        System.out.println(">> [Scheduler] 완료된 예약 상태 업데이트 시작...");

        // 현재 시간에서 유예 기간을 뺀 시점을 기준으로 완료된 예약을 조회합니다.
        LocalDateTime checkTime = LocalDateTime.now().minusMinutes(GRACE_PERIOD_MINUTES);

        // 1. 'CONFIRMED' 또는 'CARPOOL' 상태이며, 도착 시간이 지난 모든 예약을 조회합니다.
        List<Reservation> completedReservations = reservationRepository.findAllByStatusInAndArrivalDateTimeBefore(
                List.of(ReservationStatus.CONFIRMED, ReservationStatus.CARPOOL),
                checkTime
        );

        if (completedReservations.isEmpty()) {
            System.out.println(">> [Scheduler] 완료된 예약이 없습니다.");
            return;
        }

        // 2. 조회된 예약들의 상태를 'COMPLETED'로 변경합니다.
        System.out.printf(">> [Scheduler] %d개의 완료된 예약을 업데이트합니다.%n", completedReservations.size());
        for (Reservation reservation : completedReservations) {
            reservation.setStatus(ReservationStatus.COMPLETED);
        }

        // @Transactional에 의해 메소드가 종료되면 변경 사항이 DB에 자동으로 저장됩니다.
    }
}
