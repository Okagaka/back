package com.okagaka.OkaGaka.domain.carrequest.repository;

import com.okagaka.OkaGaka.domain.carrequest.entity.CarRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CarRequestRepository extends JpaRepository<CarRequest, Long> {
    // 요청 상태로 차량 요청을 조회하는 메서드 예시
    List<CarRequest> findByStatus(CarRequest.CarRequestStatus status);

    // 사용자 ID로 차량 요청을 조회하는 메서드 예시
    List<CarRequest> findByUserId(Long userId);
}
