//package com.okagaka.OkaGaka.domain.reservation.service;
//
//import com.okagaka.OkaGaka.domain.reservation.dto.CarpoolProposalResponse;
//import com.okagaka.OkaGaka.domain.reservation.enums.ProposalStatus;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.okagaka.OkaGaka.common.external.tmap.Coordinate;
//import com.okagaka.OkaGaka.common.external.tmap.TmapGeocodingClient;
//import com.okagaka.OkaGaka.domain.reservation.entity.Reservation;
//import com.okagaka.OkaGaka.domain.reservation.entity.CarpoolProposal;
//import com.okagaka.OkaGaka.domain.reservation.repository.ReservationRepository;
//import com.okagaka.OkaGaka.domain.reservation.repository.CarpoolProposalRepository;
//import com.okagaka.OkaGaka.domain.reservation.dto.ReservationResponse;
//import com.okagaka.OkaGaka.domain.reservation.dto.ReservationRequest;
//import com.okagaka.OkaGaka.domain.reservation.enums.ReservationStatus;
//import com.okagaka.OkaGaka.domain.user.entity.User;
//import com.okagaka.OkaGaka.domain.user.repository.UserRepository;
//import com.okagaka.OkaGaka.domain.familygroup.entity.FamilyGroup;
//import com.okagaka.OkaGaka.common.exception.ErrorCode;
//import com.okagaka.OkaGaka.common.exception.CustomException;
//import com.okagaka.OkaGaka.domain.tmap.service.TmapService;
//import com.okagaka.OkaGaka.domain.tmap.dto.CarpoolCheckResult;
//
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class ReservationService {
//
//    private final UserRepository userRepository;
//    private final ReservationRepository reservationRepository;
//    private final CarpoolProposalRepository carpoolProposalRepository;
//    private final TmapService tmapService;
//    private final TmapGeocodingClient tmapGeocodingClient;
//    private final NotificationService notificationService;
//
//    /**
//     * 예약 생성(기본 예약 or 카풀 제안 발생)
//     */
//    public ReservationResponse createReservation(Long userId, ReservationRequest request) {
//        try {
//
//            System.out.println("===== 예약 생성 시작 =====");
//            System.out.println("userId: " + userId);
//            System.out.println("request: " + request);
//
//            // 사용자 조회
//            User user = userRepository.findById(userId)
//                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
//            System.out.println("사용자 조회 완료: " + user.getName());
//            FamilyGroup familyGroup = user.getFamilyGroup();
//            System.out.println("FamilyGroup 조회 완료: " + (familyGroup != null ? familyGroup.getId() : "없음"));
//
//            // 출발/도착 좌표 계산
//            Coordinate DepartureCoord = tmapGeocodingClient.getCoordinates(
//                    request.getDepartureCityDo(),
//                    request.getDepartureGuGun(),
//                    request.getDepartureDong(),
//                    request.getDepartureBunji()
//            );
//            System.out.println("DepartureCoord: " + DepartureCoord);
//            System.out.println("DepartureCoord lat: " + DepartureCoord.getLat());
//            System.out.println("DepartureCoord lon: " + DepartureCoord.getLon());
//
//
//            Coordinate destinationCoord = tmapGeocodingClient.getCoordinates(
//                    request.getDestinationCityDo(),
//                    request.getDestinationGuGun(),
//                    request.getDestinationDong(),
//                    request.getDestinationBunji()
//            );
//            System.out.println("DestinationCoord: " + destinationCoord);
//
//            System.out.println("TMAP API 호출: startX=" + DepartureCoord.getLon() +
//                    ", startY=" + DepartureCoord.getLat() +
//                    ", endX=" + destinationCoord.getLon() +
//                    ", endY=" + destinationCoord.getLat() +
//                    ", arrivalTime=" + request.getArrivalTime());
//
//
//            // 출발 시간 계산
//            Map<String, Object> departureInfo = tmapService.calculateDepartureTime(
//                    destinationCoord,
//                    destinationCoord,
//                    request.getArrivalTime(),
//                    request.getDate()
//            );
//
//            LocalDateTime departureDateTime = (LocalDateTime) departureInfo.get("departureDateTime");
//            int totalTimeInSeconds = (int) departureInfo.get("totalTime");
//            System.out.println("departureDateTime: " + departureDateTime + ", totalTime: " + totalTimeInSeconds);
//
//            LocalDateTime arrivalDateTime = request.getDate().atTime(request.getArrivalTime());
//            System.out.println("arrivalDateTime: " + arrivalDateTime);
//
//            // 해당 가족의 예약 중, 시간 겹치는 예약 조회
//            List<Reservation> overlapping = reservationRepository.findOverlappingReservations(
//                    familyGroup.getId(),
//                    departureDateTime,
//                    arrivalDateTime
//            );
//            System.out.println("겹치는 예약 수: " + overlapping.size());
//
//            // 충돌 예약 없음 -> 단독 예약 확정
//            if (overlapping.isEmpty()) {
//                Reservation reservation = reservationRepository.save(
//                        Reservation.builder()
//                                .user(user)
//                                .title(request.getTitle())
//                                .departureDateTime(departureDateTime)
//                                .arrivalDateTime(arrivalDateTime)
//                                .departureLatitude(Double.parseDouble(destinationCoord.getLat()))
//                                .departureLongitude(Double.parseDouble(destinationCoord.getLon()))
//                                .destinationLatitude(Double.parseDouble(destinationCoord.getLat()))
//                                .destinationLongitude(Double.parseDouble(destinationCoord.getLon()))
//                                .travelTimeSec(totalTimeInSeconds)
//                                .status(ReservationStatus.CONFIRMED)
//                                .build()
//                );
//                System.out.println("예약 확정 완료, reservationId: " + reservation.getId());
//
//                return ReservationResponse.builder()
//                        .reservationId(reservation.getId())
//                        .status(reservation.getStatus())
//                        .calculatedDepartureTime(departureDateTime)
//                        .message("예약이 확정되었습니다.")
//                        .build();
//            } else {
//
//                // 카풀 가능성 체크
//                // 겹치는 예약 있음 → 확인할 목록 필터링 (CONFIRMED, CARPOOL만 대상으로)
////                List<Reservation> confirmedReservations = overlapping.stream()
////                        .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.CARPOOL)
////                        .collect(Collectors.toList());
//                List<Reservation> confirmedReservations = overlapping.stream()
//                        .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.CARPOOL)
//                        .toList();
//                System.out.println("확정/카풀 예약 수: " + confirmedReservations.size());
//
//                // 카풀 최대 인원 체크
//                if (confirmedReservations.size() >= 2) {
//                    System.out.println("카풀 최대 인원 초과");
//                    throw new CustomException(ErrorCode.CARPOOL_CAPACITY_EXCEEDED);
//                }
//
//                // 카풀 가능성 판단
//                CarpoolCheckResult carpoolCheck  = tmapService.canCarpoolTogether(confirmedReservations, request);
//                System.out.println("canCarpool 결과: " + carpoolCheck );
//                if (carpoolCheck == null) {
//                    throw new CustomException(ErrorCode.CARPOOL_NOT_POSSIBLE);
//                }
//
//                // 요청자 출발 시간 재계산
//                LocalDateTime calculatedDepartureDateTime = arrivalDateTime.minusSeconds(carpoolCheck.getNewReservationTravelTimeSec());
//
//                // 요청자 예약 -> PENDING
//                Reservation pendingReservation = reservationRepository.save(
//                        Reservation.builder()
//                                .user(user)
//                                .title(request.getTitle())
//                                .departureDateTime(calculatedDepartureDateTime)
//                                .arrivalDateTime(arrivalDateTime)
//                                .departureLatitude(Double.parseDouble(destinationCoord.getLat()))
//                                .departureLongitude(Double.parseDouble(destinationCoord.getLon()))
//                                .destinationLatitude(Double.parseDouble(destinationCoord.getLat()))
//                                .destinationLongitude(Double.parseDouble(destinationCoord.getLon()))
//                                .travelTimeSec(carpoolCheck.getNewReservationTravelTimeSec())
//                                .status(ReservationStatus.PENDING)
//                                .build()
//                );
//                System.out.println("Pending 예약 저장, reservationId: " + pendingReservation.getId());
//
//                // 기존 예약자들의 업데이트된 출발 시간
//                Map<Long, LocalDateTime> updatedDepartureTimes = carpoolCheck.getUpdatedDepartureTimes();
//
//                // 기존 예약자 각각에게 카풀 제안
//                for (Reservation existing : confirmedReservations) {
//
//                    LocalDateTime updatedDeparture = updatedDepartureTimes.get(existing.getId());
//
//                    // 기존 예약자의 출발 시간은 아직 확정하지 않고, 제안 정보만 저장
//                    CarpoolProposal proposal = CarpoolProposal.builder()
//                            .fromReservation(existing)
//                            .toReservation(pendingReservation)
//                            .proposedDepartureTime(updatedDeparture)
//                            .status(ProposalStatus.PENDING)
//                            .build();
//                    carpoolProposalRepository.save(proposal);
//
//                    // 알림 발송
//                    // TODO: 알림 시스템 연동 - 기존 예약자에게 알림 전송
//                    notificationService.sendNotification(
//                            existing.getUser().getId(),
//                            "카풀 제안 도착",
//                            "새로운 카풀 제안이 도착했습니다. 확인해주세요."
//                    );
//
////                    // 기존 예약의 출발 시간 업데이트
////                    LocalDateTime updatedDeparture = updatedDepartureTimes.get(existing.getId());
////                    existing.setDepartureDateTime(updatedDeparture); // 일단 그냥 업데이트 되도록 함(수락 없어도)
////
////                    // DB에 저장
////                    reservationRepository.save(existing);
////
////                    // 기존 예약자에게 카풀 제안 생성
////                    carpoolProposalRepository.save(
////                            CarpoolProposal.builder()
////                                    .fromReservation(existing)
////                                    .toReservation(pendingReservation)
////                                    .proposedDepartureTime(departureDateTime)
////                                    .status(ReservationStatus.PENDING)
////                                    .build()
////                    );
//                }
//
//                return ReservationResponse.builder()
//                        .reservationId(pendingReservation.getId())
//                        .status(pendingReservation.getStatus())
//                        .calculatedDepartureTime(departureDateTime)
//                        .message("카풀 제안이 기존 예약자에게 전송되었습니다. 승인을 기다리는 중입니다.")
//                        .build();
//            }
//
//        } catch (Exception e) {
//            System.out.println("예약 생성 중 에러 발생: " + e.getMessage());
//            e.printStackTrace();
//            throw e;
//        }
//
//        /*
//            ✅ 다음 구현 추천
//                이 구조에서 이어서 구현해야 할 핵심은:
//
//                1. 카풀 제안 승인/거절 API
//                // 사용자가 수락하면 → 해당 proposal 상태를 ACCEPTED로 변경
//                // 모두 ACCEPTED 되면 → toReservation을 CONFIRMED 상태로 변경
//                // 하나라도 REJECT되면 → toReservation을 REJECTED로 변경
//                2. 알림 시스템 (이벤트 기반 or FCM)
//                3. TmapService.canCarpoolTogether(...) 상세 구현
//                여러 개의 출발지/목적지를 고려한 병합 경로 시뮬레이션
//             */
//
//            /*
//            Reservation existing = conflictingReservations.get(0); // 하나만 처리한다고 가정
//
//            // 4-1. TMAP 경로 매트릭스로 병합 가능 여부 확인
//            boolean canMerge = tmapService.canCarpool(existing, request);
//
//            Reservation pendingReservation = reservationRepository.save(
//                    Reservation.builder()
//                            .user(user)
//                            .title(request.getTitle())
//                            .date(request.getDate())
//                            .departureTime(departureTime)
//                            .arrivalTime(request.getArrivalTime())
//                            .departure(request.getDeparture())
//                            .destination(request.getDestination())
//                            .status(ReservationStatus.PENDING)
//                            .build()
//            );
//
//            carpoolProposalRepository.save(
//                    CarpoolProposal.builder()
//                            .fromReservation(existing)
//                            .toReservation(pendingReservation)
//                            .proposedDepartureTime(LocalDateTime.of(request.getDate(), departureTime))
//                            .status(ReservationStatus.PENDING)
//                            .build()
//            );
//
//            return ReservationResponse.builder()
//                    .reservationId(pendingReservation.getId())
//                    .status(ReservationStatus.PENDING)
//                    .calculatedDepartureTime(departureTime)
//                    .message("기존 예약과 겹쳐 카풀 제안이 전송되었습니다.")
//                    .build();
//
//             */
//    }
//
//    /**
//     * 카풀 제안 수락
//     */
//    @Transactional
//    public ReservationResponse acceptCarpoolProposal(Long proposalId, Long userId) {
//        /*
//            CarpoolProposal 상태 확인 → 기존 예약자 승인 여부에 따라 요청자 예약 확정
//
//            기존 예약자 출발시간은 제안대로 반영 → CARPOOL 상태
//
//            트랜잭션 처리 → 안전하게 DB 반영
//
//            Repository에 PENDING 상태 조회 메서드 추가 → 모든 제안 수락 확인 가능
//         */
//
//        // 제안 조회
//        CarpoolProposal proposal = carpoolProposalRepository.findById(proposalId)
//                .orElseThrow(() -> new CustomException(ErrorCode.CARPOOL_PROPOSAL_NOT_FOUND));
//
//        Reservation existingReservation = proposal.getFromReservation(); // 기존 예약
//        Reservation pendingReservation = proposal.getToReservation(); // 새로운 예약
//
//        // 권한 체크: 기존 예약자만 수락 가능
//        if (!existingReservation.getUser().getId().equals(userId)) {
//            throw new CustomException(ErrorCode.UNAUTHORIZED_ACTION);
//        }
//
//        // 제안 상태 업데이트(제안 수락)
//        proposal.setStatus(ProposalStatus.ACCEPTED);
//        carpoolProposalRepository.save(proposal);
//
//        // 기존 예약 갱신(기존 예약자의 출발 시간 확정)
//        existingReservation.setDepartureDateTime(proposal.getProposedDepartureTime());
//        existingReservation.setStatus(ReservationStatus.CARPOOL);
//        reservationRepository.save(existingReservation);
//
////        // 모든 기존 예약자들이 수락했는지 확인
////        List<CarpoolProposal> pendingProposals = carpoolProposalRepository.findByToReservationAndStatus(
////                pendingReservation, ReservationStatus.PENDING
////        );
////
////        if (pendingProposals.isEmpty()) {
////            // 모든 제안 수락 완료 → 요청자 예약 확정
////            pendingReservation.setStatus(ReservationStatus.CONFIRMED);
////            reservationRepository.save(pendingReservation);
////        }
//
//        // 모든 제안이 수락되었는지 체크
//        boolean allAccepted = carpoolProposalRepository
//                .findByToReservation(pendingReservation).stream()
//                .allMatch(p -> p.getStatus() == ProposalStatus.ACCEPTED);
//
//        if (allAccepted) {
//            pendingReservation.setStatus(ReservationStatus.CONFIRMED);
//            reservationRepository.save(pendingReservation);
//
//            notificationService.sendNotification(
//                    pendingReservation.getUser().getId(),
//                    "카풀 확정",
//                    "모든 기존 예약자가 카풀을 수락했습니다. 예약이 확정되었습니다."
//            );
//        }
//
//        return ReservationResponse.builder()
//                .reservationId(existingReservation.getId())
//                .status(existingReservation.getStatus())
//                .calculatedDepartureTime(existingReservation.getDepartureDateTime())
//                .message("카풀 제안을 수락했습니다.")
//                .build();
//    }
//
//
//    /**
//     * 카풀 제안 거절
//     */
//    // 의문: 그러면 3명일 경우 3명 다 취소해야 하는 거 아님? 밑에는 두개만 인 것 같은데..
//    @Transactional
//    public void rejectCarpoolProposal(Long proposalId, Long userId) {
//        CarpoolProposal proposal = carpoolProposalRepository.findById(proposalId)
//                .orElseThrow(() -> new CustomException(ErrorCode.CARPOOL_PROPOSAL_NOT_FOUND));
//
//        Reservation existingReservation = proposal.getFromReservation();
//        Reservation requesterReservation = proposal.getToReservation();
//
//        // 권한 체크
//        if (!existingReservation.getUser().getId().equals(userId)) {
//            throw new CustomException(ErrorCode.UNAUTHORIZED_ACTION);
//        }
//
//        // 제안 거절 처리
//        proposal.setStatus(ProposalStatus.REJECTED);
//        carpoolProposalRepository.save(proposal);
//
//        // 요청자 예약 자동 취소
//        requesterReservation.setStatus(ReservationStatus.CANCELLED);
//        reservationRepository.save(requesterReservation);
//
//        // 알림 전송
//        notificationService.sendNotification(
//                requesterReservation.getUser().getId(),
//                "카풀 제안 거부",
//                "기존 예약자가 카풀 제안을 거부하여 예약이 취소되었습니다."
//        );
//    }
//
//    /**
//     * 사용자가 받은 PENDING 카풀 제안 목록 조회
//     */
//    @Transactional(readOnly = true)
//    public List<CarpoolProposalResponse> getReceivedCarpoolProposals(Long userId) {
//        // 1. 사용자의 모든 예약 조회
//        List<Reservation> userReservations = reservationRepository.findByUserId(userId);
//
//        // 2. 각 예약에 대한 PENDING 카풀 제안 조회
//        List<CarpoolProposal> pendingProposals = carpoolProposalRepository.findByFromReservationInAndStatus(
//                userReservations, ReservationStatus.PENDING
//        );
//
//        // 3. DTO 변환
//        return pendingProposals.stream()
//                .map(p -> CarpoolProposalResponse.builder()
//                        .proposalId(p.getId())
//                        .fromReservationId(p.getFromReservation().getId())
//                        .toReservationId(p.getToReservation().getId())
//                        .proposedDepartureTime(p.getProposedDepartureTime())
//                        .status(p.getStatus())
//                        .build()
//                ).collect(Collectors.toList());
//    }
//}
