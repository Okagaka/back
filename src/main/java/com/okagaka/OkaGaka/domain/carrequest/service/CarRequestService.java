package com.okagaka.OkaGaka.domain.carrequest.service;

import com.okagaka.OkaGaka.common.exception.CustomException;
import com.okagaka.OkaGaka.common.exception.ErrorCode;
import com.okagaka.OkaGaka.common.external.embedding.EmbeddingResultApiClient;
import com.okagaka.OkaGaka.common.external.embedding.EmbeddingResultRequest;
import com.okagaka.OkaGaka.common.external.tmap.Coordinate;
import com.okagaka.OkaGaka.common.external.tmap.TmapGeocodingClient;
import com.okagaka.OkaGaka.domain.aidecision.dto.AiDecisionResponse;
import com.okagaka.OkaGaka.domain.aidecision.entity.AiDecision;
import com.okagaka.OkaGaka.domain.aidecision.repository.AiDecisionRepository;
import com.okagaka.OkaGaka.domain.aidecision.service.AsyncDecisionService;
import com.okagaka.OkaGaka.domain.carrequest.dto.*;
import com.okagaka.OkaGaka.domain.carrequest.entity.CarRequest;
import com.okagaka.OkaGaka.domain.carrequest.repository.CarRequestRepository;
import com.okagaka.OkaGaka.domain.location.dto.LocationDTO;
import com.okagaka.OkaGaka.domain.location.service.LocationCacheService;
import com.okagaka.OkaGaka.domain.reservation.entity.Reservation;
import com.okagaka.OkaGaka.domain.reservation.repository.ReservationRepository;
import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.domain.user.repository.UserFaceImageRepository;
import com.okagaka.OkaGaka.domain.user.repository.UserRepository;
import com.okagaka.OkaGaka.domain.vehicle.dto.VehicleLocationDTO;
import com.okagaka.OkaGaka.domain.vehicle.dto.VehicleLocationUpdateDTO;
import com.okagaka.OkaGaka.domain.vehicle.entity.Vehicle;
import com.okagaka.OkaGaka.domain.vehicle.service.VehicleLocationService;
import com.okagaka.OkaGaka.domain.user.entity.UserFaceImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CarRequestService {

    private final CarRequestRepository carRequestRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final TmapGeocodingClient tmapGeocodingClient;
    private final AiDecisionRepository aiDecisionRepository;
    private final AsyncDecisionService asyncDecisionService;
    private final LocationCacheService locationCacheService;
    private final VehicleLocationService vehicleLocationService;
    private final UserFaceImageRepository userFaceImageRepository;
    private final EmbeddingResultApiClient embeddingResultApiClient;

    private static final Logger log = LoggerFactory.getLogger(CarRequestService.class);

    @Transactional
    public CarRequestResponse createAndProcessCarRequest(Long userId, CarRequestDto request) {
        System.out.println("\n===== [CarRequestService] 차량 요청 생성 시작 =====");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 요청자 위치/ 도착지 좌표 계산
//        Coordinate requesterCoord = tmapGeocodingClient.getCoordinates(
//                request.getRequesterCityDo(),
//                request.getRequesterGuGun(),
//                request.getRequesterDong(),
//                request.getRequesterBunji()
//        );


        // =================================================================
        // 1. 차량 사용 가능 여부 확인 로직(실시간 요청 확인: 현재 요청을 보내는 사용자의 가족 그룹에 아직 완료되지 않은 실시간 요청(CarRequest)이 있는지 확인합니다 (확인할 상태: REQUESTED, ANALYSIS_COMPLETE, CONFIRMED))
        // =================================================================
        Long familyGroupId = user.getFamilyGroup().getId();
        Long vehicleId = user.getFamilyGroup().getVehicle().getId();

        // 1. 본인의 미처리 요청 확인 (중복 요청 방지)
        List<CarRequest> selfActiveRequests = carRequestRepository.findByUserIdAndStatusIn(userId,
                List.of(CarRequest.CarRequestStatus.REQUESTED, CarRequest.CarRequestStatus.ANALYSIS_COMPLETE, CarRequest.CarRequestStatus.CONFIRMED));
        if (!selfActiveRequests.isEmpty()) {
            throw new CustomException(ErrorCode.DUPLICATE_REQUEST);
        }

        // 2. 사전 예약 확인 (사전 예약 시 카풀 절대 불가)
        List<Reservation> currentReservations = reservationRepository.findCurrentReservationsForVehicle(vehicleId, LocalDateTime.now());
        if (!currentReservations.isEmpty()) {
            throw new CustomException(ErrorCode.VEHICLE_IN_USE);
        }

        // 3. 다른 가족의 실시간 요청 확인 (카풀 가능성 탐색)
        List<CarRequest> ongoingRequests = carRequestRepository.findAllByUser_FamilyGroupIdAndStatus(
                familyGroupId, CarRequest.CarRequestStatus.CONFIRMED
        );

        // 카풀 시나리오 ID (없으면 null)
        Long originalCarRequestId = ongoingRequests.isEmpty() ? null : ongoingRequests.get(0).getId();


//        // 1-1 [빠른 확인] Redis에서 실시간 차량 상태 조회
//        Optional<VehicleLocationDTO> vehicleLocationOpt = locationCacheService.getVehicleLocation(familyGroupId, vehicleId);
//        if (vehicleLocationOpt.isPresent() && vehicleLocationOpt.get().getStatus() != Vehicle.VehicleStatus.Idle) {
//            throw new CustomException(ErrorCode.VEHICLE_IN_USE);
//        }
//
//        // 1-2. [정확한 확인] DB에서 현재 처리 중이거나 확정된 '실시간 요청'이 있는지 확인
//        List<CarRequest.CarRequestStatus> activeCarRequestStatuses = List.of(
//                CarRequest.CarRequestStatus.REQUESTED,
//                CarRequest.CarRequestStatus.ANALYSIS_COMPLETE,
//                CarRequest.CarRequestStatus.CONFIRMED
//        );
//        List<CarRequest> activeRequests = carRequestRepository.findByUser_FamilyGroup_IdAndStatusIn(familyGroupId, activeCarRequestStatuses);
//
//        if (!activeRequests.isEmpty()) {
//            // 본인이 이미 처리 중인 요청을 보낸 경우
//            if (activeRequests.stream().anyMatch(req -> req.getUser().getId().equals(userId))) {
//                throw new CustomException(ErrorCode.DUPLICATE_REQUEST);
//            }
//            // 다른 가족 구성원이 사용 중인 경우
//            throw new CustomException(ErrorCode.VEHICLE_IN_USE);
//        }
//
//        // 1-3. [정확한 확인] DB에서 현재 진행 중인 '사전 예약'이 있는지 확인
//        List<Reservation> currentReservations = reservationRepository.findCurrentReservationsForVehicle(vehicleId, LocalDateTime.now());
//        if (!currentReservations.isEmpty()) {
//            throw new CustomException(ErrorCode.VEHICLE_IN_USE);
//        }

        // 2. Redis에서 요청자의 실시간 위치 정보 조회
        LocationDTO userLocation = locationCacheService.getUserLocation(familyGroupId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_LOCATION_NOT_FOUND));

        System.out.println(">> Redis에서 사용자 위치 조회 완료: Lat=" + userLocation.getLatitude() + ", Lon=" + userLocation.getLongitude());

        Coordinate destinationCoord = tmapGeocodingClient.getCoordinates(
                request.getDestinationCityDo(),
                request.getDestinationGuGun(),
                request.getDestinationDong(),
                request.getDestinationBunji()
        );

        // 1. CarRequest를 "REQUESTED' 상태로 먼저 저장
        CarRequest carRequest = CarRequest.builder()
                .user(user)
                .requesterLongitude(userLocation.getLongitude())
                .requesterLatitude(userLocation.getLatitude())
                .destinationLongitude(Double.parseDouble(destinationCoord.getLon()))
                .destinationLatitude(Double.parseDouble(destinationCoord.getLat()))
                .status(CarRequest.CarRequestStatus.REQUESTED)
                .build();

        CarRequest savedRequest = carRequestRepository.save(carRequest);
        System.out.println(">> 차량 요청 저장 완료 (ID: " + savedRequest.getId() + ")");

        // 2. 비동기 서비스 호출하여 AI 분석 및 처리 시작
//        asyncDecisionService.processDecision(savedRequest.getId());
        // 이 메서드가 끝나면 트랜잭션이 커밋되고, 그 후에 Facade의 새 트랜잭션이 시작됨
//        asyncDecisionService.processDecision(savedRequest.getId());
//        System.out.println(">> AI 분석 서비스(비동기) 호출 완료");
        // 현재 트랜잭션이 성공적으로 커밋된 후에 비동기 로직을 실행하도록 스케줄링
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                System.out.println(">> 트랜잭션 커밋 완료. AI 분석 서비스(비동기) 호출 시작");
                asyncDecisionService.processDecision(savedRequest.getId(), originalCarRequestId);
            }
        });

        System.out.println(">> AI 분석 서비스(비동기) 호출 예약 완료");
        return CarRequestResponse.from(savedRequest);

//        return CarRequestResponseDto.builder()
//                .carRequestId(savedRequest.getId())
//                .status(savedRequest.getStatus().toString())
//                .message("차량 이용 가능 여부 분석이 시작되었습니다. 완료 시 알림이 전송됩니다.")
//                .build();

    }

    @Transactional(readOnly = true)
    public AiDecisionResponse getDecisionResult(Long carRequestId) {
        AiDecision decision = aiDecisionRepository.findByCarRequestId(carRequestId)
                .orElseThrow(() -> new CustomException(ErrorCode.DECISION_NOT_FOUND));

        CarRequest currentRequest = decision.getCarRequest();
//        Long carpoolGroupId = currentRequest.getCarpoolGroupId();

        List<CarpoolMember> carpoolMembers = new ArrayList<>();

        // 2. 만약 이 요청이 카풀 '제안' 상태(ANALYSIS_COMPLETE)라면,
        //    이미 운행중인(CONFIRMED) 다른 가족 구성원을 찾는다.
        if (currentRequest.getStatus() == CarRequest.CarRequestStatus.ANALYSIS_COMPLETE) {

            List<CarRequest> confirmedMembers = carRequestRepository.findAllByUser_FamilyGroupIdAndStatus(
                    currentRequest.getUser().getFamilyGroup().getId(),
                    CarRequest.CarRequestStatus.CONFIRMED
            );

            // 찾은 멤버가 있다면, 이 요청은 카풀 제안이다.
            if (!confirmedMembers.isEmpty()) {
                log.info("ANALYSIS_COMPLETE 상태의 카풀 제안입니다. 기존 멤버를 조회합니다.");
                carpoolMembers = confirmedMembers.stream()
                        .map(req -> CarpoolMember.from(req.getUser()))
                        .collect(Collectors.toList());
            }
        }
        // 3. 만약 이 요청이 이미 '확정'된(CONFIRMED) 카풀이라면,
        //    carpoolGroupId를 기준으로 다른 멤버를 찾는다 (기존 로직 보강).
        else if (currentRequest.getStatus() == CarRequest.CarRequestStatus.CONFIRMED && currentRequest.getCarpoolGroupId() != null) {

            List<CarRequest> allMembersInCarpool = carRequestRepository.findAllByCarpoolGroupId(currentRequest.getCarpoolGroupId());

            if (allMembersInCarpool.size() > 1) {
                log.info("CONFIRMED 상태의 카풀입니다. 다른 멤버를 조회합니다.");
                carpoolMembers = allMembersInCarpool.stream()
                        .filter(req -> !req.getId().equals(carRequestId)) // 나 자신은 제외
                        .map(req -> CarpoolMember.from(req.getUser()))
                        .collect(Collectors.toList());
            }
        }

        // 4. 조회된 멤버 정보와 함께 응답 DTO 생성
        return AiDecisionResponse.from(decision, carpoolMembers);

//        // 2. 카풀 그룹 ID가 있고, 실제 카풀인지 확인
//        if (carpoolGroupId != null) {
//            // 3. carpoolGroupId로 함께 타는 모든 구성원의 CarRequest 조회
//            List<CarRequest> allMembersInCarpool = carRequestRepository.findAllByCarpoolGroupId(carpoolGroupId);
//
//            // 4. 카풀이 맞다면 (2명 이상), '나 자신을 제외한' 다른 멤버들의 정보만 추출
//            if (allMembersInCarpool.size() > 1) {
//                carpoolMembers = allMembersInCarpool.stream()
//                        .filter(req -> !req.getId().equals(carRequestId)) // 나 자신은 제외
//                        .map(req -> CarpoolMember.from(req.getUser())) // User 정보를 DTO로 변환
//                        .collect(Collectors.toList());
//            }
//        }
//
//        // 5. decision 객체와 함께 carpoolMembers 리스트를 DTO 생성 메서드에 전달
//        return AiDecisionResponse.from(decision, carpoolMembers);
    }

    /**
     * AI 분석 결과를 바탕으로 한 사용자의 최종 선택을 처리합니다.
     *
     * @param carRequestId 차량 요청 ID
     * @param userId       사용자 ID (권한 확인용)
     * @param confirmation 사용자의 선택 정보 (VEHICLE or TRANSIT)
     */
    @Transactional
    public void confirmUserChoice(Long carRequestId, Long userId, ConfirmationRequest confirmation) {
        log.info("confirmUserChoice 시작 - carRequestId: {}, userId: {}", carRequestId, userId);

        try {
            // 1. 요청 정보 조회
            CarRequest carRequest = carRequestRepository.findById(carRequestId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CARREQUEST_NOT_FOUND));
            log.info("요청 정보 조회 성공. carRequestId: {}", carRequest.getId());

            // 2. 권한 확인
            if (!carRequest.getUser().getId().equals(userId)) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
            log.info("권한 확인 완료.");

            // 3. 상태 확인
            if (carRequest.getStatus() != CarRequest.CarRequestStatus.ANALYSIS_COMPLETE) {
                throw new CustomException(ErrorCode.INVALID_REQUEST_STATUS);
            }
            log.info("상태 확인 완료 (ANALYSIS_COMPLETE).");

            // 4. 대중교통 선택 시
            if (confirmation.choice() == ConfirmationRequest.Choice.PUBLIC_TRANSPORT) {
                carRequest.setStatus(CarRequest.CarRequestStatus.CANCELLED);
                log.info("사용자가 대중교통 선택. 상태를 CANCELLED로 변경. carRequestId: {}", carRequestId);
                return;
            }

            // 5. 차량 이용 선택 시
            log.info("사용자가 차량 이용 선택.");
            User user = carRequest.getUser();
            Vehicle vehicle = user.getFamilyGroup().getVehicle();

            // 5-1. 카풀 여부 판단
            List<CarRequest> ongoingRequests = carRequestRepository
                    .findByUser_FamilyGroupIdAndStatusAndIdNot(
                            user.getFamilyGroup().getId(), CarRequest.CarRequestStatus.CONFIRMED, carRequestId
                    );
            boolean isCarpoolConfirmation = !ongoingRequests.isEmpty();
            // 로그 추가: 카풀 여부 판단 결과
            log.info("카풀 합류 여부: {}", isCarpoolConfirmation);

            // 5-2. 예약 겹침 최종 확인
            List<Reservation> overlaps = reservationRepository.findOverlappingReservationsForVehicle(
                    vehicle.getId(), LocalDateTime.now(), carRequest.getEstimatedDestinationTime()
            );
            if (!overlaps.isEmpty()) {
                carRequest.setStatus(CarRequest.CarRequestStatus.REJECTED);
                log.warn("예약 겹침 발생으로 요청 거절. carRequestId: {}", carRequestId);
                throw new CustomException(ErrorCode.RESERVATION_CONFLICT);
            }
            log.info("예약 겹침 없음.");

            // 5-3. 단독 운행 시 다른 실시간 요청 겹침 확인
            if (!isCarpoolConfirmation && carRequestRepository.existsByUser_FamilyGroupIdAndStatus(user.getFamilyGroup().getId(), CarRequest.CarRequestStatus.CONFIRMED)) {
                carRequest.setStatus(CarRequest.CarRequestStatus.REJECTED);
                log.warn("단독 운행 확정 시도 중 다른 실시간 요청이 이미 확정됨. carRequestId: {}", carRequestId);
                throw new CustomException(ErrorCode.VEHICLE_ALREADY_IN_USE);
            }
            log.info("다른 실시간 요청 겹침 없음.");

//            // ✅ 새로운 POST 요청 로직 추가
//            // 사용자 얼굴 이미지의 Embedding URL 조회
//            List<String> embeddingUrls = userFaceImageRepository.findByUserId(userId).stream()
//                    .map(UserFaceImage::getEmbeddingImageUrl)
//                    .collect(Collectors.toList());
//
//            // POST 요청을 위한 DTO 생성
//            EmbeddingResultRequest requestDto = EmbeddingResultRequest.builder()
//                    .userId(String.valueOf(userId))
//                    .vehicleId(String.valueOf(vehicle.getId()))
//                    .embeddingUrls(embeddingUrls)
//                    .build();
//
//            System.out.println(requestDto);
//
//
//            // API 호출
//            embeddingResultApiClient.sendEmbeddingResult(requestDto);
//            log.info("Embedding 결과 API 호출 완료. userId: {}, vehicleId: {}", userId, vehicle.getId());

            // 6. 시나리오 분기
            if (isCarpoolConfirmation) {
                // [ 6-A. 카풀 합류 시나리오 ]
                log.info("카풀 합류 시나리오 시작.");

                VehicleLocationDTO vehicleLocation = locationCacheService.getVehicleLocation(
                        user.getFamilyGroup().getId(), vehicle.getId()
                ).orElseThrow(() -> new CustomException(ErrorCode.VEHICLE_LOCATION_NOT_FOUND));
                log.debug("현재 차량 위치 조회 성공: Lat {}, Lon {}", vehicleLocation.getLatitude(), vehicleLocation.getLongitude());

                List<CarRequest> allCarpoolMembers = new ArrayList<>(ongoingRequests);
                allCarpoolMembers.add(carRequest);

                log.info("최적 카풀 경로 재탐색 시작...");
                Optional<CarpoolSolution> solutionOpt =
                        asyncDecisionService.calculateOptimalCarpoolSolution(allCarpoolMembers, vehicleLocation);

                if (solutionOpt.isEmpty()) {
                    carRequest.setStatus(CarRequest.CarRequestStatus.REJECTED);
                    log.warn("최종 확정 단계에서 유효한 카풀 경로를 찾지 못함. carRequestId: {}", carRequestId);
                    throw new CustomException(ErrorCode.CARPOOL_NOT_AVAILABLE_ANYMORE);
                }
                log.info("최적 카풀 경로 재탐색 성공.");

                CarpoolSolution solution = solutionOpt.get();
                Map<CarpoolPoint, LocalDateTime> timetable = solution.getTimetable();

                log.info("기존 이용자들의 도착 시간 업데이트 시작...");
                for (CarRequest originalReq : ongoingRequests) {
                    LocalDateTime newDestinationTime = timetable.entrySet().stream()
                            .filter(entry -> !entry.getKey().isPickup() && entry.getKey().getCarRequest().getId().equals(originalReq.getId()))
                            .map(Map.Entry::getValue).findFirst().orElse(originalReq.getEstimatedDestinationTime());
                    log.debug("기존 요청 ID: {}, 기존 도착시간: {}, 새 도착시간: {}", originalReq.getId(), originalReq.getEstimatedDestinationTime(), newDestinationTime);
                    originalReq.setEstimatedDestinationTime(newDestinationTime);
                }

                Long groupId = ongoingRequests.get(0).getCarpoolGroupId();
                carRequest.setCarpoolGroupId(groupId);
                carRequest.setStatus(CarRequest.CarRequestStatus.CONFIRMED);
                log.info("카풀 합류 처리 완료. carRequestId: {}, carpoolGroupId: {}", carRequestId, groupId);

            } else {
                // [ 6-B. 단독 운행 시작 시나리오 ]
                log.info("단독 운행 시작 시나리오 시작.");

                log.info("차량 상태를 'Moving'으로 변경합니다.");
                VehicleLocationUpdateDTO statusUpdateDto = new VehicleLocationUpdateDTO();
                statusUpdateDto.setStatus(Vehicle.VehicleStatus.Moving);
                // ...

                statusUpdateDto.setLatitude(vehicle.getVehicleLatitude());
                statusUpdateDto.setLongitude(vehicle.getVehicleLongitude());
                statusUpdateDto.setBatteryLevel(vehicle.getBatteryLevel());
                statusUpdateDto.setSpeed(vehicle.getSpeed());

                vehicleLocationService.updateAndBroadcastLocation(vehicle.getId(), statusUpdateDto);

                carRequest.setStatus(CarRequest.CarRequestStatus.CONFIRMED);
                log.info("요청 상태를 'CONFIRMED'로 변경.");

                carRequestRepository.flush();
                carRequest.setCarpoolGroupId(carRequest.getId());
                log.info("단독 운행 처리 완료. carRequestId: {}, carpoolGroupId: {}", carRequestId, carRequest.getId());
            }
        } catch (Exception e) {
            // ✅ [가장 중요] 예외 발생 시 로그 기록
            log.error("!!! confirmUserChoice 처리 중 심각한 오류 발생. carRequestId: {}", carRequestId, e);
            // 트랜잭션 롤백 및 에러 응답을 위해 예외를 다시 던짐
            throw e;
        }
    }
}

//    /**
//     * AI 분석 결과를 바탕으로 한 사용자의 최종 선택을 처리합니다.
//     * @param carRequestId 차량 요청 ID
//     * @param userId       사용자 ID (권한 확인용)
//     * @param confirmation 사용자의 선택 정보 (VEHICLE or TRANSIT)
//     */
//    @Transactional
//    public void confirmUserChoice(Long carRequestId, Long userId, ConfirmationRequest confirmation) {
//        // 1. 요청 정보 조회
//        CarRequest carRequest = carRequestRepository.findById(carRequestId)
//                .orElseThrow(() -> new CustomException(ErrorCode.CARREQUEST_NOT_FOUND));
//
//        // 2. 권한 확인: 요청을 보낸 사용자와 확정하려는 사용자가 동일한지 확인
//        if (!carRequest.getUser().getId().equals(userId)) {
//            throw new CustomException(ErrorCode.FORBIDDEN);
//        }
//
//        // 3. 상태 확인: AI 분석이 완료된 상태인지 확인
//        if (carRequest.getStatus() != CarRequest.CarRequestStatus.ANALYSIS_COMPLETE) {
//            throw new CustomException(ErrorCode.INVALID_REQUEST_STATUS);
//        }
//
//        // 4. 사용자의 선택에 따라 로직 분기
//        if (confirmation.choice() == ConfirmationRequest.Choice.VEHICLE) {
//            // 4-1. [중요] 차량 이용 선택 시, 최종적으로 예약 겹침을 다시 확인 (경쟁 상태 방지)
//            AiDecision decision = carRequest.getAiDecision();
//            List<Reservation> overlaps = reservationRepository.findOverlappingReservationsForVehicle(
//                    carRequest.getUser().getFamilyGroup().getVehicle().getId(),
//                    LocalDateTime.now(), // 지금부터
//                    decision.getEstimatedDestinationTime() // 예상 도착 시간까지
//            );
//
//            if (!overlaps.isEmpty()) {
//                // 최종 확인 순간에 다른 예약이 생긴 경우, 요청을 거절 상태로 변경
//                carRequest.setStatus(CarRequest.CarRequestStatus.REJECTED);
//                throw new CustomException(ErrorCode.RESERVATION_CONFLICT);
//            }
//
//            // ✅ 5. 차량 상태를 'Moving'으로 업데이트
//            System.out.println(">> 차량 상태를 Moving으로 변경합니다 (ID: " + carRequestId + ")");
//
//            // 5-1. 업데이트할 차량 객체 가져오기
//            Vehicle vehicle = carRequest.getUser().getFamilyGroup().getVehicle();
//
//            // 5-2. VehicleLocationService에 보낼 DTO 생성
//            VehicleLocationUpdateDTO statusUpdateDto = new VehicleLocationUpdateDTO();
//            statusUpdateDto.setStatus(Vehicle.VehicleStatus.Moving);
//            // 상태만 변경하고, 나머지 정보(위치, 속도 등)는 현재 차량의 DB 값으로 유지
//            statusUpdateDto.setLatitude(vehicle.getVehicleLatitude());
//            statusUpdateDto.setLongitude(vehicle.getVehicleLongitude());
//            statusUpdateDto.setBatteryLevel(vehicle.getBatteryLevel());
//            statusUpdateDto.setSpeed(vehicle.getSpeed());
//
//            // 5-3. 차량 상태 업데이트 서비스 호출 (DB, Redis, WebSocket 모두 업데이트됨)
//            vehicleLocationService.updateAndBroadcastLocation(vehicle.getId(), statusUpdateDto);
//
//            // ✅ 6. 차량 요청을 최종 확정 상태로 변경
//            carRequest.setStatus(CarRequest.CarRequestStatus.CONFIRMED);
//            System.out.println(">> 차량 요청 최종 확정 (ID: " + carRequestId + ")");
//
//
//        } else {
//            // 4-2. 대중교통 선택 시, 요청을 '취소' 상태로 변경
//            carRequest.setStatus(CarRequest.CarRequestStatus.CANCELLED);
//            System.out.println(">> 차량 요청 취소됨 (ID: " + carRequestId + ")");
//        }
//    }

