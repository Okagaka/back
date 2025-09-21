package com.okagaka.OkaGaka.domain.carrequest.repository;

import com.okagaka.OkaGaka.domain.carrequest.entity.CarRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CarRequestRepository extends JpaRepository<CarRequest, Long> {
    // 요청 상태로 차량 요청을 조회하는 메서드 예시
    List<CarRequest> findByStatus(CarRequest.CarRequestStatus status);

    // 사용자 ID로 차량 요청을 조회하는 메서드 예시
    List<CarRequest> findByUserId(Long userId);

    // 특정 상태이면서, 예상 도착 시간이 현재 시간보다 이전인 모든 요청을 찾는 메서드
    List<CarRequest> findAllByStatusAndEstimatedDestinationTimeBefore(CarRequest.CarRequestStatus status, LocalDateTime now);

    /**
     * 특정 가족 그룹 내에서 주어진 상태들에 해당하는 모든 차량 요청을 조회합니다.
     * @param familyGroupId 가족 그룹 ID
     * @param statuses 조회할 요청 상태 목록 (예: REQUESTED, ACCEPTED)
     * @return 활성 상태인 차량 요청 목록
     */
    List<CarRequest> findByUser_FamilyGroup_IdAndStatusIn(Long familyGroupId, List<CarRequest.CarRequestStatus> statuses);

    Optional<CarRequest> findByUser_FamilyGroupIdAndStatus(Long familyGroupId, CarRequest.CarRequestStatus status);

    List<CarRequest> findAllByUser_FamilyGroupIdAndStatus(Long familyGroupId, CarRequest.CarRequestStatus status);

    List<CarRequest> findByUserIdAndStatusIn(Long userId, List<CarRequest.CarRequestStatus> statuses);

    List<CarRequest> findByUser_FamilyGroupIdAndStatusAndIdNot(Long familyGroupId, CarRequest.CarRequestStatus status, Long carRequestId);

    boolean existsByUser_FamilyGroupIdAndStatus(Long familyGroupId, CarRequest.CarRequestStatus status);

    // carpoolGroupId로 모든 관련 요청을 찾는 메서드
    List<CarRequest> findAllByCarpoolGroupId(Long carpoolGroupId);
}
