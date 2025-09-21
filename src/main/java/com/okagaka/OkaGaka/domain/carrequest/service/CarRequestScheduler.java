package com.okagaka.OkaGaka.domain.carrequest.service;

import com.okagaka.OkaGaka.domain.carrequest.entity.CarRequest;
import com.okagaka.OkaGaka.domain.carrequest.repository.CarRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CarRequestScheduler {

    private final CarRequestRepository carRequestRepository;

    private int gracePeriodMinutes = 10; // 예상 도착 시간보다 10분 늦는 경우 고려

    // ✅ 5분마다 한 번씩 실행 (fixedRate = 밀리초 단위)
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void updateCompletedCarRequests() {
        System.out.println(">> [Scheduler] 완료된 차량 요청 상태 업데이트 시작...");

        LocalDateTime checkTime = LocalDateTime.now().minusMinutes(gracePeriodMinutes);

        // 1. 예상 도착 시간이 과거인 'ACCEPTED' 상태의 모든 요청을 조회
        List<CarRequest> completedRequests = carRequestRepository.findAllByStatusAndEstimatedDestinationTimeBefore(
                CarRequest.CarRequestStatus.CONFIRMED,
                checkTime
        );

        if (completedRequests.isEmpty()) {
            System.out.println(">> [Scheduler] 완료된 요청이 없습니다.");
            return;
        }

        // 2. 조회된 요청들의 상태를 'COMPLETED'로 변경
        System.out.printf(">> [Scheduler] %d개의 완료된 요청을 업데이트합니다.%n", completedRequests.size());
        for (CarRequest request : completedRequests) {
            request.setStatus(CarRequest.CarRequestStatus.COMPLETED);
        }
        // @Transactional에 의해 메서드가 끝나면 변경 사항이 DB에 자동으로 저장됩니다.
    }
}
