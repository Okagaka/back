package com.okagaka.OkaGaka.domain.reservation.service;

import com.okagaka.OkaGaka.domain.reservation.dto.CarpoolProposalResponse;
import com.okagaka.OkaGaka.domain.reservation.enums.ProposalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.okagaka.OkaGaka.common.external.tmap.Coordinate;
import com.okagaka.OkaGaka.common.external.tmap.TmapGeocodingClient;
import com.okagaka.OkaGaka.domain.reservation.entity.Reservation;
import com.okagaka.OkaGaka.domain.reservation.entity.CarpoolProposal;
import com.okagaka.OkaGaka.domain.reservation.repository.ReservationRepository;
import com.okagaka.OkaGaka.domain.reservation.repository.CarpoolProposalRepository;
import com.okagaka.OkaGaka.domain.reservation.dto.ReservationResponse;
import com.okagaka.OkaGaka.domain.reservation.dto.ReservationRequest;
import com.okagaka.OkaGaka.domain.reservation.enums.ReservationStatus;
import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.domain.user.repository.UserRepository;
import com.okagaka.OkaGaka.domain.familygroup.entity.FamilyGroup;
import com.okagaka.OkaGaka.common.exception.ErrorCode;
import com.okagaka.OkaGaka.common.exception.CustomException;
import com.okagaka.OkaGaka.domain.tmap.service.TmapService;
import com.okagaka.OkaGaka.domain.tmap.dto.CarpoolCheckResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final CarpoolProposalRepository carpoolProposalRepository;
    private final TmapService tmapService;
    private final TmapGeocodingClient tmapGeocodingClient;
    private final NotificationService notificationService;

    private final int MAX_CARPOL_SIZE = 3;

    /**
     * 예약 생성(단독 확정 or 카풀 제안 발생)
     * - 겹치는 예약이 없으면 CONFIRMED
     * - 겹치는 예약이 있으면 PENDING + 기존 예약자들에게 CarpoolProposal 생성
     */
    public ReservationResponse createReservation(Long userId, ReservationRequest request) {
        try {

            System.out.println("===== 예약 생성 시작 =====");
            System.out.println("userId: " + userId);
            System.out.println("request: " + request);

            // 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            System.out.println("사용자 조회 완료: " + user.getName());
            FamilyGroup familyGroup = user.getFamilyGroup();
            System.out.println("FamilyGroup 조회 완료: " + (familyGroup != null ? familyGroup.getId() : "없음"));

            // 출발/도착 좌표 계산
            Coordinate departureCoord = tmapGeocodingClient.getCoordinates(
                    request.getDepartureCityDo(),
                    request.getDepartureGuGun(),
                    request.getDepartureDong(),
                    request.getDepartureBunji()
            );
            System.out.println("DepartureCoord: " + departureCoord);
            System.out.println("DepartureCoord lat: " + departureCoord.getLat());
            System.out.println("DepartureCoord lon: " + departureCoord.getLon());


            Coordinate destinationCoord = tmapGeocodingClient.getCoordinates(
                    request.getDestinationCityDo(),
                    request.getDestinationGuGun(),
                    request.getDestinationDong(),
                    request.getDestinationBunji()
            );
            System.out.println("DestinationCoord: " + destinationCoord);

            System.out.println("TMAP API 호출: startX=" + departureCoord.getLon() +
                    ", startY=" + departureCoord.getLat() +
                    ", endX=" + destinationCoord.getLon() +
                    ", endY=" + destinationCoord.getLat() +
                    ", arrivalTime=" + request.getArrivalTime());


            // 출발 시간 계산
            Map<String, Object> departureInfo = tmapService.calculateDepartureTime(
                    departureCoord,
                    destinationCoord,
                    request.getArrivalTime(),
                    request.getDate()
            );

            LocalDateTime departureDateTime = (LocalDateTime) departureInfo.get("departureDateTime"); // 요청자의 계산된 출발 시간
            int totalTimeSec = (int) departureInfo.get("totalTime"); // totalTimeInSeconds
            System.out.println("departureDateTime: " + departureDateTime + ", totalTime: " + totalTimeSec);

            LocalDateTime arrivalDateTime = request.getDate().atTime(request.getArrivalTime()); // 요청자의 도착 시간
            System.out.println("arrivalDateTime: " + arrivalDateTime);

            // 가족 내 시간 겹침 조회
            List<Reservation> overlapping = reservationRepository.findOverlappingReservations(
                    familyGroup.getId(),
                    departureDateTime,
                    arrivalDateTime
            );
            System.out.println("겹치는 예약 수: " + overlapping.size());

            // PENDING 겹침이 있으면 새 요청도 대기(또는 비즈니스에 따라 바로 실패 처리)
            boolean hasWaiting = overlapping.stream().anyMatch(r -> r.getStatus() == ReservationStatus.PENDING);
            if (hasWaiting) {
                throw new CustomException(ErrorCode.CARPOOL_DECISION_PENDING); // 필요 시 새 에러코드 정의
            }

            // 충돌 예약 없음 -> 단독 예약 확정
            if (overlapping.isEmpty()) {
                Reservation reservation = reservationRepository.save(
                        Reservation.builder()
                                .user(user)
                                .title(request.getTitle())
                                .departureDateTime(departureDateTime)
                                .arrivalDateTime(arrivalDateTime)
                                .departureLatitude(Double.parseDouble(departureCoord.getLat()))
                                .departureLongitude(Double.parseDouble(departureCoord.getLon()))
                                .destinationLatitude(Double.parseDouble(destinationCoord.getLat()))
                                .destinationLongitude(Double.parseDouble(destinationCoord.getLon()))
                                .travelTimeSec(totalTimeSec)
                                .status(ReservationStatus.CONFIRMED)
                                .build()
                );
                System.out.println("예약 확정 완료, reservationId: " + reservation.getId());

                return ReservationResponse.builder()
                        .reservationId(reservation.getId())
                        .status(reservation.getStatus())
                        .calculatedDepartureTime(departureDateTime)
                        .message("예약이 확정되었습니다.")
                        .build();
            }

                // 카풀 가능성 체크(겹침이 있으면 카풀 가능성 판단)
                List<Reservation> confirmedReservations = overlapping.stream()
                        .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.CARPOOL)
                        .toList();
                System.out.println("확정/카풀 예약 수: " + confirmedReservations.size());

                // 최대 동승자(기존 2명) 초과 체크
                if (confirmedReservations.size() >= MAX_CARPOL_SIZE) {
                    System.out.println("카풀 최대 인원 초과");
                    throw new CustomException(ErrorCode.CARPOOL_CAPACITY_EXCEEDED);
                }

                // 카풀 가능성 판단
                CarpoolCheckResult carpoolCheck  = tmapService.canCarpoolTogether(confirmedReservations, request);

                // ------------------------
                // 최적 카풀 경로 탐색 (optimizeCarpool)
                // ------------------------
//                CarpoolService carpoolService = new CarpoolService(tmapService);
//                CarpoolCheckResult carpoolCheck = carpoolService.optimizeCarpool(confirmedReservations, request);
                System.out.println("canCarpool 결과: " + carpoolCheck );

                if (carpoolCheck == null) {
                    throw new CustomException(ErrorCode.CARPOOL_NOT_POSSIBLE);
                }

                // 요청자 출발 시간 재계산
                LocalDateTime calculatedDepartureDateTime = arrivalDateTime.minusSeconds(carpoolCheck.getNewReservationTravelTimeSec());

                // 요청자 예약을 PENDING으로 저장
                Reservation pendingReservation = reservationRepository.save(
                        Reservation.builder()
                                .user(user)
                                .title(request.getTitle())
                                .departureDateTime(calculatedDepartureDateTime)
                                .arrivalDateTime(arrivalDateTime)
                                .departureLatitude(Double.parseDouble(departureCoord.getLat()))
                                .departureLongitude(Double.parseDouble(departureCoord.getLon()))
                                .destinationLatitude(Double.parseDouble(destinationCoord.getLat()))
                                .destinationLongitude(Double.parseDouble(destinationCoord.getLon()))
                                .travelTimeSec(carpoolCheck.getNewReservationTravelTimeSec())
                                .status(ReservationStatus.PENDING)
                                .build()
                );
                System.out.println("Pending 예약 저장, reservationId: " + pendingReservation.getId());

                // 기존 예약자들의 업데이트된 출발 시간
                Map<Long, LocalDateTime> updatedDepartureTimes = carpoolCheck.getUpdatedDepartureTimes();

                // 기존 예약자 각각에게 카풀 제안
                for (Reservation existing : confirmedReservations) {

                    LocalDateTime updatedDeparture = updatedDepartureTimes.get(existing.getId()); // 기존 예약 업데이트된 시간

                    // 기존 예약자의 출발 시간은 아직 확정하지 않고, 제안 정보만 저장
                    CarpoolProposal proposal = CarpoolProposal.builder()
                            .fromReservation(existing)
                            .toReservation(pendingReservation)
                            .proposedDepartureTime(updatedDeparture)
                            .status(ProposalStatus.PENDING)
                            .build();
                    carpoolProposalRepository.save(proposal);

                    // 알림 발송
                    // TODO: 알림 시스템 연동 - 기존 예약자에게 알림 전송
                    notificationService.sendNotification(
                            existing.getUser().getId(),
                            "카풀 제안 도착",
                            "새로운 카풀 제안이 도착했습니다. 확인해주세요."
                    );


                }

                return ReservationResponse.builder()
                        .reservationId(pendingReservation.getId())
                        .status(pendingReservation.getStatus())
                        .calculatedDepartureTime(calculatedDepartureDateTime)
                        .message("카풀 제안이 기존 예약자에게 전송되었습니다. 승인을 기다리는 중입니다.")
                        .build();


        } catch (Exception e) {
            System.out.println("예약 생성 중 에러 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * DFS 기반 최적 3인 카풀 계산 로직
     */
//    public CarpoolCheckResult optimizeCarpool(List<Reservation> confirmedReservations, ReservationRequest newRequest) {
//        // 1. 좌표 변환
//        Coordinate newDeparture = tmapGeocodingClient.getCoordinates(
//                newRequest.getDepartureCityDo(),
//                newRequest.getDepartureGuGun(),
//                newRequest.getDepartureDong(),
//                newRequest.getDepartureBunji()
//        );
//        Coordinate newDestination = tmapGeocodingClient.getCoordinates(
//                newRequest.getDestinationCityDo(),
//                newRequest.getDestinationGuGun(),
//                newRequest.getDestinationDong(),
//                newRequest.getDestinationBunji()
//        );
//
//        // 2. 모든 후보 예약 모으기
//        List<Reservation> allReservations = new ArrayList<>(confirmedReservations);
//        Reservation newRes = Reservation.builder()
//                .departureLatitude(Double.parseDouble(newDeparture.getLat()))
//                .departureLongitude(Double.parseDouble(newDeparture.getLon()))
//                .destinationLatitude(Double.parseDouble(newDestination.getLat()))
//                .destinationLongitude(Double.parseDouble(newDestination.getLon()))
//                .arrivalDateTime(newRequest.getDate().atTime(newRequest.getArrivalTime()))
//                .build();
//        allReservations.add(newRes);
//
//
//    }



    /**
     * 카풀 제안 수락
     */
    @Transactional
    public ReservationResponse acceptCarpoolProposal(Long proposalId, Long userId) {
        /*
            CarpoolProposal 상태 확인 → 기존 예약자 승인 여부에 따라 요청자 예약 확정

            기존 예약자 출발시간은 제안대로 반영 → CARPOOL 상태

            트랜잭션 처리 → 안전하게 DB 반영

            Repository에 PENDING 상태 조회 메서드 추가 → 모든 제안 수락 확인 가능
         */

        // 제안 조회
        CarpoolProposal proposal = carpoolProposalRepository.findById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARPOOL_PROPOSAL_NOT_FOUND));

        Reservation existingReservation = proposal.getFromReservation(); // 기존 예약
        Reservation pendingReservation = proposal.getToReservation(); // 새로운 예약

        // 권한 체크: 기존 예약자만 수락 가능
        if (!existingReservation.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        // 제안 상태 업데이트(제안 수락)
        proposal.setStatus(ProposalStatus.ACCEPTED);
        carpoolProposalRepository.save(proposal);

        // pendingReservation에 연결된 모든 제안 조회
        List<CarpoolProposal> allProposals = carpoolProposalRepository
                .findByToReservation(pendingReservation);

        // 모든 기존 예약자가 제안을 수락했는지 체크
        boolean allAccepted = allProposals.stream()
                .allMatch(p -> p.getStatus() == ProposalStatus.ACCEPTED);

        if (allAccepted) {
            // 모든 수락 완료 시점에 기존 예약자 출발시간 확정 + 상태 CARPOOL
            for (CarpoolProposal p : allProposals) {
                Reservation res = p.getFromReservation();
                res.setDepartureDateTime(p.getProposedDepartureTime());
                res.setStatus(ReservationStatus.CARPOOL);
                reservationRepository.save(res);
            }

            // 요청자 예약 확정
            pendingReservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(pendingReservation);

            // 알림 전송
            notificationService.sendNotification(
                    pendingReservation.getUser().getId(),
                    "카풀 확정",
                    "모든 기존 예약자가 카풀을 수락했습니다. 예약이 확정되었습니다."
            );
        }

        // 응답 반환
        return ReservationResponse.builder()
                .reservationId(pendingReservation.getId())
                .status(pendingReservation.getStatus())
                .calculatedDepartureTime(pendingReservation.getDepartureDateTime())
                .message("카풀 제안을 수락했습니다.")
                .build();
    }


    /**
     * 카풀 제안 거절
     */
    // 의문: 그러면 3명일 경우 3명 다 취소해야 하는 거 아님? 밑에는 두개만 인 것 같은데..
    @Transactional
    public ReservationResponse rejectCarpoolProposal(Long proposalId, Long userId) {

        // 거부할 제안 조회
        CarpoolProposal proposal = carpoolProposalRepository.findById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARPOOL_PROPOSAL_NOT_FOUND));

        Reservation existingReservation = proposal.getFromReservation(); // 거부한 기존 예약자
        Reservation pendingReservation = proposal.getToReservation(); // 요청자 예약

        // 권한 체크: 기존 예약자만 거부 가능
        if (!existingReservation.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        // pendingReservation에 연결된 모든 proposal 조회
        List<CarpoolProposal> allProposals = carpoolProposalRepository.findByToReservation(pendingReservation);

        // 모든 proposal 상태 REJECTED 처리
        for (CarpoolProposal p : allProposals) {
            p.setStatus(ProposalStatus.REJECTED);
            carpoolProposalRepository.save(p);
        }

        // 요청자 예약 자동 취소
        pendingReservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(pendingReservation);

        // 알림 전송
        notificationService.sendNotification(
                pendingReservation.getUser().getId(),
                "카풀 제안 거부",
                "기존 예약자 중 한 명이 카풀 제안을 거부하여 예약이 취소되었습니다."
        );

        return ReservationResponse.builder()
                .reservationId(pendingReservation.getId())
                .status(pendingReservation.getStatus())
                .calculatedDepartureTime(pendingReservation.getDepartureDateTime())
                .message("카풀 제안이 거부되어 예약이 취소되었습니다.")
                .build();
    }

    /**
     * 사용자가 받은 PENDING 카풀 제안 목록 조회
     */
    // 나의 예약명, 제안한 사람
    @Transactional(readOnly = true)
    public List<CarpoolProposalResponse> getReceivedCarpoolProposals(Long userId) {


        List<CarpoolProposal> pendingProposals =
                carpoolProposalRepository.findPendingProposalsForUser(userId, ProposalStatus.PENDING);


        // DTO 변환 (null 체크 포함)
        return pendingProposals.stream()
                .map(p -> {
                    Long fromId = p.getFromReservation() != null ? p.getFromReservation().getId() : null;
                    Long toId = p.getToReservation() != null ? p.getToReservation().getId() : null;

                    return CarpoolProposalResponse.builder()
                            .proposalId(p.getId())
                            .fromReservationId(fromId)
                            .toReservationId(toId)
                            .fromReservationTitle(p.getFromReservation().getTitle())
                            .toReservationUserName(p.getToReservation().getUser().getName())
                            .proposedDepartureTime(p.getProposedDepartureTime())
                            .status(p.getStatus())
                            .build();
                })
                .collect(Collectors.toList());
//        // 1. 사용자의 모든 예약 조회
//        List<Reservation> userReservations = reservationRepository.findByUserId(userId);
//
//        // 1-1. 예약이 없으면 바로 빈 리스트 반환
//        if (userReservations.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        // 2. 각 예약에 대한 PENDING 카풀 제안 조회
//        List<CarpoolProposal> pendingProposals = carpoolProposalRepository.findByFromReservationInAndStatus(
//                userReservations, ReservationStatus.PENDING
//        );
//
//        // 3. DTO 변환 (null 체크 포함)
//        return pendingProposals.stream()
//                .map(p -> {
//                    Long fromId = p.getFromReservation() != null ? p.getFromReservation().getId() : null;
//                    Long toId = p.getToReservation() != null ? p.getToReservation().getId() : null;
//
//                    return CarpoolProposalResponse.builder()
//                            .proposalId(p.getId())
//                            .fromReservationId(fromId)
//                            .toReservationId(toId)
//                            .proposedDepartureTime(p.getProposedDepartureTime())
//                            .status(p.getStatus())
//                            .build();
//                })
//                .collect(Collectors.toList());
    }
}
