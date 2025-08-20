package com.okagaka.OkaGaka.domain.reservation.service;

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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final TmapService tmapService;
    private final CarpoolProposalRepository carpoolProposalRepository;
    private final TmapGeocodingClient tmapGeocodingClient;

    public ReservationResponse createReservation(ReservationRequest request) {

        // 1. 사용자 조회
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        FamilyGroup familyGroup = user.getFamilyGroup();

        // 2. 출발 시간 estimateTravelTime계산 (도착 시간 - 예상 소요 시간)
        Coordinate DepartureCoord = tmapGeocodingClient.getCoordinates(
                request.getDepartureCityDo(),
                request.getDepartureGuGun(),
                request.getDepartureDong(),
                request.getDepartureBunji()
        );

        Coordinate destinationCoord = tmapGeocodingClient.getCoordinates(
                request.getDestinationCityDo(),
                request.getDestinationGuGun(),
                request.getDestinationDong(),
                request.getDestinationBunji()
        );

//        LocalDateTime calculatedDepartureTime = tmapService.calculateDepartureTime(DepartureCoord, destinationCoord, request.getArrivalTime(), request.getDate());
////        LocalTime calculatedDepartureTime = request.getArrivalTime().minusMinutes(estimatedMinutes);
//
//        LocalDateTime departureDateTime;
//        if (calculatedDepartureTime.isAfter(request.getArrivalTime())) {
//            // 출발 시간이 도착 시간보다 늦다는 건 전날 출발이라는 의미
//            departureDateTime = request.getDate().minusDays(1).atTime(calculatedDepartureTime);
//        } else {
//            departureDateTime = request.getDate().atTime(calculatedDepartureTime);
//        }
//        LocalDateTime departureDateTime = tmapService.calculateDepartureTime(
//                DepartureCoord,
//                destinationCoord,
//                request.getArrivalTime(),
//                request.getDate()
//        );

        Map<String, Object> departureInfo = tmapService.calculateDepartureTime(
                DepartureCoord,
                destinationCoord,
                request.getArrivalTime(),
                request.getDate()
        );

        LocalDateTime departureDateTime = (LocalDateTime) departureInfo.get("departureDateTime");
        int totalTimeInSeconds = (int) departureInfo.get("totalTime");


        // 출발 시각
//        LocalDateTime departureDateTime = calculatedDepartureDateTime;



        LocalDateTime arrivalDateTime = request.getDate().atTime(request.getArrivalTime());

//        long duration = java.time.Duration.between(departureDateTime, arrivalDateTime).getSeconds();


        // 3. 해당 가족의 예약 중, 시간 겹치는 예약 조회
//        List<Reservation> overlapping = reservationRepository.findByFamilyGroupAndDateAndTimeOverlap(
//                familyGroup.getId(), request.getDate(), calculatedDepartureTime, request.getArrivalTime());
        List<Reservation> overlapping = reservationRepository.findOverlappingReservations(
                familyGroup.getId(),
                departureDateTime,
                arrivalDateTime
        );

        // 4. 충돌 예약 없음 -> 단독 예약 확정
        if (overlapping.isEmpty()) {
            Reservation reservation = reservationRepository.save(
                    Reservation.builder()
                            .user(user)
                            .title(request.getTitle())
//                            .date(request.getDate())
//                            .departureTime(calculatedDepartureTime)
//                            .arrivalTime(request.getArrivalTime())
                            .departureDateTime(departureDateTime)
                            .arrivalDateTime(arrivalDateTime)
                            .departureLatitude(Double.parseDouble(DepartureCoord.getLat()))
                            .departureLongitude(Double.parseDouble(DepartureCoord.getLon()))
                            .destinationLatitude(Double.parseDouble(destinationCoord.getLat()))
                            .destinationLongitude(Double.parseDouble(destinationCoord.getLon()))
                            .travelTimeSec(totalTimeInSeconds)
                            .status(ReservationStatus.CONFIRMED)
                            .build()
            );

            return ReservationResponse.builder()
                    .reservationId(reservation.getId())
                    .status(reservation.getStatus())
                    .calculatedDepartureTime(departureDateTime)
                    .message("예약이 확정되었습니다.")
                    .build();
        } else {

            // 5. 겹치는 예약 있음 → 확인할 목록 필터링 (CONFIRMED, CARPOOL만 대상으로)
            List<Reservation> confirmedReservations = overlapping.stream()
                    .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.CARPOOL)
                    .collect(Collectors.toList());

            // 6. 카풀 최대 인원 체크
            if (confirmedReservations.size() >= 2) {
                throw new CustomException(ErrorCode.CARPOOL_CAPACITY_EXCEEDED);
            }

            // 7. 카풀 가능성 판단
            Integer canCarpool = tmapService.canCarpoolTogether(confirmedReservations, request);


            if (canCarpool == null) {
                throw new CustomException(ErrorCode.CARPOOL_NOT_POSSIBLE);
            }


            // 8. 새 예약을 Pending 상태로 저장
            Reservation pendingReservation = reservationRepository.save(
                    Reservation.builder()
                            .user(user)
                            .title(request.getTitle())
//                            .date(request.getDate())
//                            .departureTime(calculatedDepartureTime)
//                            .arrivalTime(request.getArrivalTime())
                            .departureDateTime(departureDateTime)
                            .arrivalDateTime(arrivalDateTime)
                            .departureLatitude(Double.parseDouble(DepartureCoord.getLat()))
                            .departureLongitude(Double.parseDouble(DepartureCoord.getLon()))
                            .destinationLatitude(Double.parseDouble(destinationCoord.getLat()))
                            .destinationLongitude(Double.parseDouble(destinationCoord.getLon()))
//                            .travelTimeSec() // 여기 수정해야 함
                            .status(ReservationStatus.PENDING)
                            .build()
            );

            // 9. 기존 예약자 각각에게 카풀 제안 (CarpoolProposal 생성)
            // "기존 예약자 각각에게 당신의 예약과 겹치는 새로운 사람이 있으니, 카풀 제안할게요. 괜찮으면 승인해줘요." 라는 의도로 CarpoolProposal을 저장하는 로직
            for (Reservation existing  : confirmedReservations) {
                carpoolProposalRepository.save(
                        CarpoolProposal.builder()
                                .fromReservation(existing)
                                .toReservation(pendingReservation)
//                                .proposedDepartureTime(LocalDateTime.of(request.getDate(), calculatedDepartureTime))
                                .proposedDepartureTime(departureDateTime)
                                .status(ReservationStatus.PENDING)
                                .build()
                );

                // TODO: 알림 시스템 연동 - 기존 예약자에게 알림 전송

            }

            return ReservationResponse.builder()
                    .reservationId(pendingReservation.getId())
                    .status(pendingReservation.getStatus())
                    .calculatedDepartureTime(departureDateTime)
                    .message("카풀 제안이 기존 예약자에게 전송되었습니다. 승인을 기다리는 중입니다.")
                    .build();

            /*
            ✅ 다음 구현 추천
                이 구조에서 이어서 구현해야 할 핵심은:

                1. 카풀 제안 승인/거절 API
                // 사용자가 수락하면 → 해당 proposal 상태를 ACCEPTED로 변경
                // 모두 ACCEPTED 되면 → toReservation을 CONFIRMED 상태로 변경
                // 하나라도 REJECT되면 → toReservation을 REJECTED로 변경
                2. 알림 시스템 (이벤트 기반 or FCM)
                3. TmapService.canCarpoolTogether(...) 상세 구현
                여러 개의 출발지/목적지를 고려한 병합 경로 시뮬레이션
             */

            /*
            Reservation existing = conflictingReservations.get(0); // 하나만 처리한다고 가정

            // 4-1. TMAP 경로 매트릭스로 병합 가능 여부 확인
            boolean canMerge = tmapService.canCarpool(existing, request);

            Reservation pendingReservation = reservationRepository.save(
                    Reservation.builder()
                            .user(user)
                            .title(request.getTitle())
                            .date(request.getDate())
                            .departureTime(departureTime)
                            .arrivalTime(request.getArrivalTime())
                            .departure(request.getDeparture())
                            .destination(request.getDestination())
                            .status(ReservationStatus.PENDING)
                            .build()
            );

            carpoolProposalRepository.save(
                    CarpoolProposal.builder()
                            .fromReservation(existing)
                            .toReservation(pendingReservation)
                            .proposedDepartureTime(LocalDateTime.of(request.getDate(), departureTime))
                            .status(ReservationStatus.PENDING)
                            .build()
            );

            return ReservationResponse.builder()
                    .reservationId(pendingReservation.getId())
                    .status(ReservationStatus.PENDING)
                    .calculatedDepartureTime(departureTime)
                    .message("기존 예약과 겹쳐 카풀 제안이 전송되었습니다.")
                    .build();

             */
        }
    }
}
