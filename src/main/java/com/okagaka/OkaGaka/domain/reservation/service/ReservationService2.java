//package com.okagaka.OkaGaka.domain.reservation.service;
//
//import com.okagaka.OkaGaka.common.exception.CustomException;
//import com.okagaka.OkaGaka.common.exception.ErrorCode;
//import com.okagaka.OkaGaka.domain.reservation.dto.ReservationRequest;
//import com.okagaka.OkaGaka.domain.reservation.dto.ReservationResponse;
//import com.okagaka.OkaGaka.domain.reservation.entity.Reservation;
//import com.okagaka.OkaGaka.domain.reservation.enums.ReservationStatus;
//import com.okagaka.OkaGaka.domain.reservation.repository.ReservationRepository;
//import com.okagaka.OkaGaka.domain.reservation.repository.CarpoolProposalRepository;
//import com.okagaka.OkaGaka.domain.user.repository.UserRepository;
//import com.okagaka.OkaGaka.domain.user.entity.User;
//import com.okagaka.OkaGaka.domain.familygroup.entity.FamilyGroup;
//import com.okagaka.OkaGaka.domain.tmap.service.TmapService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import java.time.LocalTime;
//import java.util.List;
//
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class ReservationService2 {
//
//    private final ReservationRepository reservationRepository;
//    private final UserRepository userRepository;
//    private final TmapService tmapService;
//    private final CarpoolProposalRepository carpoolProposalRepository;
//
//    public ReservationResponse createReservation(ReservationRequest request) {
//
//        User user = userRepository.findById(request.getUserId())
//                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
//        FamilyGroup familyGroup = user.getFamilyGroup();
//
//        // 1. 출발 시간 계산 (도착 시간 - 예상 소요 시간)
//        int estimatedMinutes = tmapService.estimateTravelTime(request.getDeparture(), request.getDestination());
//        LocalTime departureTime = request.getArrivalTime().minusMinutes(estimatedMinutes); // 꼭 이렇게 빼야 하는 건지는 tmap api 보고 결정할 수 있을 듯 -> 응답에 포함되어 있을 수도 있음
//
//        // 2. 가족 차량의 기존 예약과 충돌 확인
//        List<Reservation> conflictingReservations = reservationRepository.findByFamilyGroupAndDateAndTimeOverlap(familyGroup.getId(), request.getDate(), departureTime, request.getArrivalTime());
//
//        if(conflictingReservations.isEmpty()) {
//            // 3. 충돌 없음 -> 바로 예약 확정
//            Reservation reservation = reservationRepository.save(
//                    Reservation.builder()
//                            .user(user)
//                            .title(request.getTitle())
//                            .date(request.getDate())
//                            .departureTime(departureTime)
//                            .arrivalTime(request.getArrivalTime())
//                            .departure(request.getDeparture())
//                            .destination(request.getDestination())
//                            .status(ReservationStatus.CONFIRMED)
//                            .build()
//            );
//
//            return ReservationResponse.builder()
//                    .reservationId(reservation.getId())
//                    .status(reservation.getStatus())
//                    .calculatedDepartureTime(departureTime)
//                    .message("예약이 확정되었습니다.")
//                    .build();
//        }
//
//        else {
//
//            // 1. 새 예약 임시 생성(status는 PENDING)
//            Reservation newReservation = Reservation.builder()
//                    .user(user)
//                    .title(request.getTitle())
//                    .date(request.getDate())
//                    .departureTime(departureTime)
//                    .arrivalTime(request.getArrivalTime())
//                    .departure(request.getDeparture())
//                    .destination(request.getDestination())
//                    .status(ReservationStatus.PENDING)
//                    .build();
//
//            // 2. 기존 예약을 CarpoolGroup별로 묶기
//            Map<CarpoolGroup, List<Reservation>> groupedByCarpool = groupReservationsByCarpoolGroup(conflictingReservations);
//
//            // 4. 각 그룹에 새 예약 포함시켜 카풀 가능 여부 검사
//            for (Map.Entry<CarpoolGroup, List<Reservation>> entry : groupedByCarpool.entrySet()) {
//                List<Reservation> groupReservations = entry.getValue();
//
//                // 최대 인원 제한 확인
//                if (groupReservations.size() + 1 > 4) {
//                    throw new CustomException(ErrorCode.CARPOOL_MAXIMUM_EXCEEDED);
//                }
//
//                List<Reservation> combinedGroup = new ArrayList<>(groupReservations);
//                combinedGroup.add(newReservation);
//
//                // TMAP API로 그룹 전체 카풀 가능 여부 판단
//                boolean canCarpool = tmapService.canCarpoolGroup(combinedGroup);
//                if (!canCarpool) {
//                    throw new CustomException(ErrorCode.CARPOOL_NOT_POSSIBLE_WITH_EXISTING);
//                }
//            }
//
//            // 5. 그룹 충돌 없으면 CarpoolGroup 새로 생성 또는 병합 (간단히 새 그룹 생성 예시)
//            CarpoolGroup newCarpoolGroup = CarpoolGroup.builder()
//                    .familyGroup(familyGroup)
//                    .status(CarpoolGroup.CarpoolGroupStatus.PENDING)
//                    .build();
//
//            carpoolGroupRepository.save(newCarpoolGroup);
//
//            // 6. 새 예약에 그룹 연동 및 저장
//            newReservation.setCarpoolGroup(newCarpoolGroup);
//            reservationRepository.save(newReservation);
//
//            // 7. (추가) 알림 처리 및 승인 대기 프로세스
//
//            return ReservationResponse.builder()
//                    .reservationId(newReservation.getId())
//                    .status(newReservation.getStatus())
//                    .calculatedDepartureTime(departureTime)
//                    .message("카풀 제안이 생성되었습니다.")
//                    .build();
//        }
//
//        private LocalTime calculateDepartureTime(ReservationRequest request) {
//            int estimatedMinutes = tmapService.estimateTravelTime(request.getDeparture(), request.getDestination());
//            return request.getArrivalTime().minusMinutes(estimatedMinutes);
//        }
//
//        private Map<CarpoolGroup, List<Reservation>> groupReservationsByCarpoolGroup(List<Reservation> reservations) {
//            return reservations.stream()
//                    .collect(Collectors.groupingBy(r -> r.getCarpoolGroup() != null ? r.getCarpoolGroup() : createDummyGroup()));
//        }
//
//        private CarpoolGroup createDummyGroup() {
//            // 예약 그룹이 없는 단독 예약용 임시 그룹(구현 편의용)
//            CarpoolGroup dummyGroup = new CarpoolGroup();
//            dummyGroup.setId(-1L); // 임의 ID (DB에 저장 안 함)
//            return dummyGroup;
//        }
//    }
//
//            // 3. 각 그룹에 새 예약 포함시켜 카풀 가능 여부 검사
//
////            // 4. 충돌 존재 -> 카풀 제안
////            Reservation existing = conflictingReservations.get(0); // 하나만 처리한다고 가정(이러면 안 될 것 같은데)
////
////            // 4-1 TMAP 경로 매트릭스로 병합 가능 여부 확인
////            boolean canMerge = tmapService.canCarpool(existing, request);
////
////            Reservation pendingReservation = reservationRepository.save(
////                    Reservation.builder()
////                            .user(user)
////                            .title(request.getTitle())
////                            .date(request.getDate())
////                            .departureTime(departureTime)
////                            .arrivalTime(request.getArrivalTime())
////                            .departure(request.getDeparture())
////                            .destination(request.getDestination())
////                            .status(ReservationStatus.PENDING)
////                            .build()
////            );
////
////            carpoolProposalRepository.save(
////                    CarpoolProposal.builder()
////                            .fromReservation(existing)
////                            .toReservation(pendingReservation)
////                            .proposedDepartureTime(LocalDateTime.of(request.getDate(), departureTime))
////                            .status(ReservationStatus.PENDING)
////                            .build()
////            );
////
////            return ReservationResponse.builder()
////                    .reservationId(pendingReservation.getId())
////                    .status(ReservationStatus.PENDING)
////                    .calculatedDepartureTime(departureTime)
////                    .message("기존 예약과 겹쳐 카풀 제안이 전송되었습니다.")
////                    .build();
////        }
////
////    }
//}
