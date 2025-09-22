package com.okagaka.OkaGaka.domain.aidecision.service;

import com.okagaka.OkaGaka.common.exception.CustomException;
import com.okagaka.OkaGaka.common.exception.ErrorCode;
import com.okagaka.OkaGaka.common.external.openai.OpenAiClient;
import com.okagaka.OkaGaka.common.external.tmap.Coordinate;
import com.okagaka.OkaGaka.domain.aidecision.dto.OpenAiDecisionResponse;
import com.okagaka.OkaGaka.domain.aidecision.dto.WeatherInfo;
import com.okagaka.OkaGaka.domain.aidecision.entity.AiDecision;
import com.okagaka.OkaGaka.domain.aidecision.repository.AiDecisionRepository;
import com.okagaka.OkaGaka.domain.carrequest.dto.CarpoolPoint;
import com.okagaka.OkaGaka.domain.carrequest.dto.CarpoolSolution;
import com.okagaka.OkaGaka.domain.carrequest.entity.CarRequest;
import com.okagaka.OkaGaka.domain.carrequest.repository.CarRequestRepository;
import com.okagaka.OkaGaka.domain.familygroup.entity.FamilyGroup;
import com.okagaka.OkaGaka.domain.location.dto.LocationDTO;
import com.okagaka.OkaGaka.domain.location.service.LocationCacheService;
import com.okagaka.OkaGaka.domain.reservation.entity.Reservation;
import com.okagaka.OkaGaka.domain.reservation.repository.ReservationRepository;
import com.okagaka.OkaGaka.domain.tmap.dto.ArrivalTimes;
import com.okagaka.OkaGaka.domain.tmap.dto.TransitInfo;
import com.okagaka.OkaGaka.domain.tmap.service.TmapService;
import com.okagaka.OkaGaka.domain.tmap.service.TransitService;
import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.domain.vehicle.dto.VehicleLocationDTO;
import com.okagaka.OkaGaka.domain.vehicle.entity.Vehicle;
import jdk.jfr.TransitionFrom;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsyncDecisionService {

    private final CarRequestRepository carRequestRepository;
    private final ReservationRepository reservationRepository;
    private final AiDecisionRepository aiDecisionRepository;
    private final TmapService tmapService;
    private final TransitService transitService;
    private final LocationCacheService locationCacheService;
    private final WeatherService weatherService;
    private final OpenAiClient openAiClient;

    private static final int MAX_CARPOOL_DELAY_MINUTES = 20;
    private static final int MAX_CARPOOL_USERS = 3;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    // 1. originalCarRequestId 파라미터 추가
    public void processDecision(Long newCarRequestId, Long originalCarRequestId) {
        // 카풀 시나리오인지 단독 운행 시나리오인지 확인
        if (originalCarRequestId != null) {
            System.out.println("\n===== [AsyncDecisionService] 카풀 가능성 분석 시작 (New ID: " + newCarRequestId + ") =====");
//            processCarpoolDecision(newCarRequestId, originalCarRequestId);
            processCarpoolDecision(newCarRequestId);
        } else {
            System.out.println("\n===== [AsyncDecisionService] AI 분석 시작 (ID: " + newCarRequestId + ") =====");
            processSingleDecision(newCarRequestId);
        }
    }

    private void processSingleDecision(Long carRequestId) {
        CarRequest carRequest = carRequestRepository.findById(carRequestId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARREQUEST_NOT_FOUND));

        try {

            User user = carRequest.getUser();
            FamilyGroup familyGroup = user.getFamilyGroup();
            Vehicle vehicle = familyGroup.getVehicle();
            Long groupId = familyGroup.getId();
            Long vehicleId = vehicle.getId();
            Long userId = user.getId();

            // Redis에서 차량의 실시간 위치/상태 정보 조회
            Optional<VehicleLocationDTO> vehicleLocationOptional = locationCacheService.getVehicleLocation(groupId, vehicleId);

            // Redis에서 요청자의 실시간 위치 정보 조회
            Optional<LocationDTO> userLocationOptional = locationCacheService.getUserLocation(groupId, userId);

            // 차량 또는 요청자의 위치 정보가 없는 경우 요청 거절
            if (vehicleLocationOptional.isEmpty()) {
                System.out.println(">> [규칙 거절] 차량의 실시간 위치 정보를 찾을 수 없음 (Redis)");
                rejectRequest(carRequest, AiDecision.DecisionResult.REJECT, "차량의 현재 위치를 확인할 수 없습니다.");
                return;
            }

            if (userLocationOptional.isEmpty()) {
                System.out.println(">> [규칙 거절] 요청자의 실시간 위치 정보를 찾을 수 없음 (Redis)");
                rejectRequest(carRequest, AiDecision.DecisionResult.REJECT, "요청자의 현재 위치를 확인할 수 없습니다.");
                return;
            }

            // 실시간 정보 DTO 가져오기
            VehicleLocationDTO realTimeVehicleLocation = vehicleLocationOptional.get();
            LocationDTO realTimeUserLocation = userLocationOptional.get(); // 요청자 위치 DTO 가져오기

            // 규칙 1: Redis의 실시간 차량 상태 확인 -> 나중에 카풀 확인으로 바꿀 거임
            if (realTimeVehicleLocation.getStatus() != Vehicle.VehicleStatus.Idle) {
                System.out.println(">> [규칙 거절] 차량이 유휴 상태가 아님 (실시간): " + realTimeVehicleLocation.getStatus());
                rejectRequest(carRequest, AiDecision.DecisionResult.REJECT, "차량을 현재 다른 가족 구성원이 이용 중입니다."); // 사실 충전 중일 때도 있음...
                return;
            }

            // Redis에서 가져온 차량의 실시간 위치 정보
            double vehicleLat = realTimeVehicleLocation.getLatitude();
            double vehicleLon = realTimeVehicleLocation.getLongitude();

            // Redis에서 가져온 요청자의 실시간 위치 정보
            double userLat = realTimeUserLocation.getLatitude();
            double userLon = realTimeUserLocation.getLongitude();

            // CarRequest에서 목적지 위치 정보 가져오기
            double destLat = carRequest.getDestinationLatitude();
            double destLon = carRequest.getDestinationLongitude();

            System.out.printf(">> [정보] 사용자 위치: (%.4f, %.4f), 차량 위치: (%.4f, %.4f)\n", userLat, userLon, vehicleLat, vehicleLon);

            // TMAP 차량 운행 시 정보 조회(다중 경유지 API)
            // ==============================================================================

            // 1. TMAP API 호출에 필요한 Coordinate 객체 3개 생성
            Coordinate vehicleCoord = new Coordinate(String.valueOf(vehicleLat), String.valueOf(vehicleLon));
            Coordinate requesterCoord = new Coordinate(String.valueOf(userLat), String.valueOf(userLon));
            Coordinate destinationCoord = new Coordinate(String.valueOf(destLat), String.valueOf(destLon));

            ArrivalTimes arrivalTimes;
            try {
                // 2. TmapService를 호출하여 예상 도착 시간 정보 조회
                System.out.println(">> [TMAP] 경로 및 도착 시간 조회 시작...");
                arrivalTimes = tmapService.getArrivalTimes(vehicleCoord, requesterCoord, destinationCoord);

                // arrivalTimes가 null인 경우 (경로 탐색 실패) 처리
                if (arrivalTimes == null) {
                    rejectRequest(carRequest, AiDecision.DecisionResult.REJECT, "차량 이동 경로를 찾을 수 없습니다.");
                    return;
                }
            } catch (RuntimeException e) {

                // ✅ 어떤 에러가 발생했는지 정확히 확인하기 위해 로그를 추가합니다.
                System.err.println("!!! TMAP API 호출 중 심각한 오류 발생 !!!");
                e.printStackTrace(); // 👈 이 코드가 에러의 전체 내용을 콘솔에 출력해 줍니다.

                // TmapClient에서 API 호출 실패시 RuntimeException이 발생하므로 여기서 처리
                rejectRequest(carRequest, AiDecision.DecisionResult.REJECT, "이동 경로를 계산하는 중 오류가 발생했습니다.");
                return;

            }

            // 3. 결과 확인 및 로깅
            LocalDateTime timeAtRequester = arrivalTimes.timeAtRequester();
            LocalDateTime timeAtDestination = arrivalTimes.timeAtDestination();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            System.out.println(">> [TMAP 결과] 요청자 위치 도착 예상 시간: " + timeAtRequester.format(formatter));
            System.out.println(">> [TMAP 결과] 최종 목적지 도착 예상 시간: " + timeAtDestination.format(formatter));
            performFinalAnalysisAndSaveDecision(carRequest, arrivalTimes.timeAtRequester(), arrivalTimes.timeAtDestination());
        } catch (
                Exception e) {
            // 이 부분은 tmapService.getArrivalTimes 등에서 발생하는 예외를 처리
            System.err.println("!! 단독 운행 경로 분석 중 오류 발생: " + e.getMessage());
            rejectRequest(carRequest, AiDecision.DecisionResult.REJECT, "경로를 계산하는 중 오류가 발생했습니다.");
        }
    }



    /**
     * 카풀 시나리오의 의사결정 프로세스
     */
    private void processCarpoolDecision(Long newCarRequestId) {
        CarRequest newCarRequest = carRequestRepository.findById(newCarRequestId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARREQUEST_NOT_FOUND));
        List<CarRequest> confirmedRequests = carRequestRepository.findAllByUser_FamilyGroupIdAndStatus(
                newCarRequest.getUser().getFamilyGroup().getId(), CarRequest.CarRequestStatus.CONFIRMED);

        if (confirmedRequests.size() >= MAX_CARPOOL_USERS) {
            rejectRequest(newCarRequest, AiDecision.DecisionResult.REJECT, "최대 탑승 인원을 초과하여 카풀을 이용할 수 없습니다.");
            return;
        }

        VehicleLocationDTO vehicleLocation = locationCacheService.getVehicleLocation(
                newCarRequest.getUser().getFamilyGroup().getId(),
                newCarRequest.getUser().getFamilyGroup().getVehicle().getId()
        ).orElseThrow(() -> new CustomException(ErrorCode.VEHICLE_LOCATION_NOT_FOUND));

        List<CarRequest> allCarpoolMembers = new ArrayList<>(confirmedRequests);
        allCarpoolMembers.add(newCarRequest);

        // [로그 추가] calculateOptimalCarpoolSolution 호출 전 데이터 확인
        System.out.println("\n--- [1. 최적 경로 탐색 시작] ---");
        System.out.println(">> 현재 차량 위치: Lat=" + vehicleLocation.getLatitude() + ", Lon=" + vehicleLocation.getLongitude());
        System.out.println(">> 카풀 멤버 후보 (총 " + allCarpoolMembers.size() + "명): " +
                allCarpoolMembers.stream().map(CarRequest::getId).collect(Collectors.toList()));

        Optional<CarpoolSolution> bestSolutionOpt = calculateOptimalCarpoolSolution(allCarpoolMembers, vehicleLocation);

        if (bestSolutionOpt.isPresent()) {
            CarpoolSolution bestSolution = bestSolutionOpt.get();
            System.out.println(">> [카풀 제안] 최적 경로 발견! 사용자에게 제안합니다.");

            // 2인 카풀일 때만 순차 배차 규칙 적용
            if (confirmedRequests.size() == 1) {
                CarpoolPoint firstPoint = bestSolution.getPath().get(0);
                boolean isFirstPointNewUserPickup = firstPoint.isPickup() && firstPoint.getCarRequest().getId().equals(newCarRequestId);

                if (!isFirstPointNewUserPickup) {
                    System.out.println(">> [순차 배차] 2인 카풀 시나리오에서 픽업이 먼저 오지 않습니다. 카풀 불가로 판단합니다.");

                    CarRequest originalUserRequest = confirmedRequests.get(0);
                    LocalDateTime originalUserArrivalTime = bestSolution.getTimetable().get(new CarpoolPoint(originalUserRequest, false, 0, 0));

                    String rejectReason = String.format("현재 차량이 다른 가족을 운행 중입니다. 해당 차량은 약 %s에 운행을 마칠 예정입니다. " +
                                    "이 시간 이후에 다시 요청해 주세요.",
                            originalUserArrivalTime.format(DateTimeFormatter.ofPattern("HH시 mm분")));

                    rejectRequest(newCarRequest, AiDecision.DecisionResult.REJECT, rejectReason);
                    return;
                }
            }

            // [로그 추가] 최종 선택된 경로와 시간표 출력
            System.out.println("\n--- [5. 최종 결과] ---");
            System.out.println(">> [성공] 최적 카풀 경로 발견!");
            System.out.println("   - 경로 순서: " + bestSolution.getPath().stream()
                    .map(p -> "ReqID:" + p.getCarRequest().getId() + (p.isPickup() ? "(픽업)" : "(목적지)"))
                    .collect(Collectors.joining(" -> ")));
            System.out.println("   - 총 소요 시간: " + bestSolution.getTotalDurationMinutes() + "분");
            System.out.println("   - 상세 시간표:");
            bestSolution.getTimetable().forEach((point, time) ->
                    System.out.printf("      - ReqID:%d %s -> 도착시간: %s\n",
                            point.getCarRequest().getId(),
                            (point.isPickup() ? "(픽업)" : "(목적지)"),
                            time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))));

            CarpoolPoint newRequesterPickupPoint = new CarpoolPoint(newCarRequest, true, newCarRequest.getRequesterLatitude(), newCarRequest.getRequesterLongitude());
            CarpoolPoint newRequesterDestinationPoint = new CarpoolPoint(newCarRequest, false, newCarRequest.getDestinationLatitude(), newCarRequest.getDestinationLongitude());

            LocalDateTime pickupTime = bestSolution.getTimetable().get(newRequesterPickupPoint);
            LocalDateTime destinationTime = bestSolution.getTimetable().get(newRequesterDestinationPoint);

            if (pickupTime == null || destinationTime == null) {
                rejectRequest(newCarRequest, AiDecision.DecisionResult.REJECT, "계산된 경로에서 요청자 정보를 찾을 수 없습니다.");
                return;
            }

            performFinalAnalysisAndSaveDecision(newCarRequest, pickupTime, destinationTime);

        } else {
            System.out.println(">> [카풀 거절] 허용 시간을 초과하지 않는 경로를 찾지 못함.");
            rejectRequest(newCarRequest, AiDecision.DecisionResult.REJECT, "카풀 시 기존 이용자의 도착 시간이 20분 이상 지연되어 요청이 거절되었습니다.");
        }
    }

    /**
     * DFS를 사용하여 카풀 경로의 모든 조합을 생성합니다.
     */
    private void generateCarpoolPaths(List<CarpoolPoint> currentPath, List<CarpoolPoint> remainingPoints,
                                      Map<CarRequest, CarpoolPoint> pickupMap, List<List<CarpoolPoint>> result) {
        if (remainingPoints.isEmpty()) {
            result.add(new ArrayList<>(currentPath));
            return;
        }

        for (CarpoolPoint p : new ArrayList<>(remainingPoints)) {
            // 목적지를 추가하려면, 짝이 되는 출발지가 이미 경로에 포함되어 있어야 함
            if (!p.isPickup()) { // p가 목적지인 경우
                CarpoolPoint pickup = pickupMap.get(p.getCarRequest());
                // 신규 요청자의 목적지는, 신규 요청자의 픽업 포인트가 경로에 있어야만 추가 가능
                if (pickup != null && !currentPath.contains(pickup)) {
                    continue; // 픽업 지점 없이 목적지만 추가될 수 없으므로 건너뛰기
                }
            }

            currentPath.add(p);
            List<CarpoolPoint> nextRemaining = new ArrayList<>(remainingPoints);
            nextRemaining.remove(p);
            generateCarpoolPaths(currentPath, nextRemaining, pickupMap, result);
            currentPath.remove(currentPath.size() - 1);
        }
    }


    /**
     * 모든 경로 후보를 순회하며 제약조건을 만족하는 최적의 경로를 찾습니다.
     */
    private Optional<CarpoolSolution> findBestCarpoolPath(List<List<CarpoolPoint>> allPaths,
                                                          VehicleLocationDTO vehicleLocation,
                                                          List<CarRequest> originalRequests) {
        List<CarpoolSolution> validSolutions = new ArrayList<>();
        Coordinate startCoord = new Coordinate(String.valueOf(vehicleLocation.getLatitude()), String.valueOf(vehicleLocation.getLongitude()));
        // 수정: 일관된 비교를 위해 기준 시간을 한 번만 생성
        LocalDateTime calculationTime = LocalDateTime.now();

        System.out.println("\n--- [4. 경로별 시간 분석 및 제약 조건 검사] ---");
        int validPathCount = 0;

        for (List<CarpoolPoint> path : allPaths) {
            String pathStr = path.stream()
                    .map(p -> "ReqID:" + p.getCarRequest().getId() + (p.isPickup() ? "(픽업)" : "(목적지)"))
                    .collect(Collectors.joining(" -> "));
            System.out.println("\n>> [검사 시작] 경로: " + pathStr);

            try {
                // 수정: 생성된 기준 시간(calculationTime)을 사용
                Map<CarpoolPoint, LocalDateTime> timetable = tmapService.getVerifiedCarpoolTimetable(startCoord, path, calculationTime);

                boolean isPathValid = true;
                for (CarRequest originalReq : originalRequests) {
                    CarpoolPoint originalDestinationPoint = path.stream()
                            .filter(p -> !p.isPickup() && p.getCarRequest().getId().equals(originalReq.getId()))
                            .findFirst().orElse(null);

                    if (originalDestinationPoint == null) {
                        isPathValid = false;
                        break;
                    }

                    LocalDateTime newArrivalTime = timetable.get(originalDestinationPoint);
                    LocalDateTime originalArrivalTime = originalReq.getEstimatedDestinationTime();

                    if (newArrivalTime == null || originalArrivalTime == null || newArrivalTime.isAfter(originalArrivalTime.plusMinutes(MAX_CARPOOL_DELAY_MINUTES))) {
                        long delayMinutes = (newArrivalTime != null && originalArrivalTime != null) ?
                                Duration.between(originalArrivalTime, newArrivalTime).toMinutes() : -1;
                        System.out.printf("   - [탈락] ReqID:%d의 지연 시간 초과 (원래: %s, 변경: %s, 지연: %d분)\n",
                                originalReq.getId(),
                                originalArrivalTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                newArrivalTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                delayMinutes);
                        isPathValid = false;
                        break;
                    }
                }

                if (isPathValid) {
                    validPathCount++;
                    System.out.println("   - [성공] 모든 제약 조건 통과. 유효 경로로 추가됨.");
                    LocalDateTime finalArrivalTime = timetable.get(path.get(path.size() - 1));
                    // 수정: 총 소요 시간 계산 시에도 동일한 기준 시간을 사용
                    long totalDuration = Duration.between(calculationTime, finalArrivalTime).toMinutes();
                    validSolutions.add(new CarpoolSolution(path, timetable, totalDuration));
                }

            } catch (Exception e) {
                System.err.println("!! TMAP API 호출 중 오류 발생 (경로: " + path + ") : " + e.getMessage());
            }
        }

        System.out.println("\n>> 총 " + validPathCount + "개의 유효 경로를 찾음.");

        return validSolutions.stream()
                .min(Comparator.comparingLong(CarpoolSolution::getTotalDurationMinutes));
    }

    /**
     * [공통 로직] 차량 이용 시간 정보가 확정된 후, 대중교통/날씨 등 추가 정보를 종합하여
     * 최종 AI 분석을 수행하고 결과를 저장하는 메서드.
     * @param carRequest 분석 대상이 되는 차량 요청
     * @param estimatedPickupTime 확정된 예상 픽업 시간
     * @param estimatedDestinationTime 확정된 예상 목적지 도착 시간
     */
    private void performFinalAnalysisAndSaveDecision(CarRequest carRequest, LocalDateTime estimatedPickupTime, LocalDateTime estimatedDestinationTime) {
        System.out.println(">> [AI 최종 분석] 차량 vs 대중교통 비교 분석 시작 (ID: " + carRequest.getId() + ")");
        try {
            User user = carRequest.getUser();
            Vehicle vehicle = user.getFamilyGroup().getVehicle();

            // 1. 대중교통 정보 조회 (API 제한으로 임시 비활성화)
            System.out.println(">> [임시] 대중교통 API 호출을 건너뜁니다.");
            TransitInfo transitInfo = null; // API 호출 대신 null을 할당

//            // 1. 대중교통 정보 조회
//            TransitInfo transitInfo = transitService.getTransitInfo(
//                    String.valueOf(carRequest.getRequesterLongitude()),
//                    String.valueOf(carRequest.getRequesterLatitude()),
//                    String.valueOf(carRequest.getDestinationLongitude()),
//                    String.valueOf(carRequest.getDestinationLatitude())
//            );

            if (transitInfo == null) {
                System.out.println(">> 조회된 대중교통 경로가 없습니다.");
            }

            // 2. 예약 시간 겹침 최종 확인 (안전장치)
            LocalDateTime estimatedStart = LocalDateTime.now();
            List<Reservation> overlaps = reservationRepository.findOverlappingReservationsForVehicle(
                    vehicle.getId(), estimatedStart, estimatedDestinationTime
            );
            if (!overlaps.isEmpty()) {
                System.out.println(">> [규칙 거절] 기존 예약과 시간 겹침 발생");
                rejectRequest(carRequest, AiDecision.DecisionResult.REJECT, "해당 시간대에 다른 가족의 예약이 이미 존재합니다.");
                return;
            }

            // 3. 날씨 정보 가져오기
            WeatherInfo weatherInfo = weatherService.getCurrentWeatherInfo(
                    String.valueOf(carRequest.getRequesterLatitude()),
                    String.valueOf(carRequest.getRequesterLongitude())
            );
            System.out.println(">> [날씨 정보] 현재 날씨: " + weatherInfo.description() + ", 온도: " + weatherInfo.temperature() + "°C");

            // 4. OpenAI 의사결정 API 호출
            System.out.println(">> [OpenAI] 의사결정 프롬프트 생성 및 API 호출 시작...");
            // ArrivalTimes 객체는 createOpenAiPrompt, saveDecision 에서 사용되므로 임시 생성
            ArrivalTimes carArrivalTimes = new ArrivalTimes(estimatedPickupTime, estimatedDestinationTime);
            String prompt = createOpenAiPrompt(user, estimatedStart, carArrivalTimes, transitInfo, weatherInfo);
            OpenAiDecisionResponse aiResponse = openAiClient.getDecision(prompt);

            // 5. AI 응답에 따른 최종 결정 및 모든 정보 저장
            // AI 추천과 상관없이, 계산된 차량 예상 시간 정보를 CarRequest에 먼저 저장
            carRequest.setEstimatedTimes(estimatedPickupTime, estimatedDestinationTime);
            carRequest.setStatus(CarRequest.CarRequestStatus.ANALYSIS_COMPLETE);

            // AI의 결정(VEHICLE 또는 PUBLIC_TRANSPORT)에 따라 DecisionResult 결정
            AiDecision.DecisionResult result = aiResponse.decision().equalsIgnoreCase("Vehicle")
                    ? AiDecision.DecisionResult.VEHICLE // AI가 차량 이용 '추천'
                    : AiDecision.DecisionResult.PUBLIC_TRANSPORT; // AI가 대중교통 '추천'

            // 모든 분석 결과를 AiDecision에 저장
            saveDecision(carRequest, result, aiResponse.reason(), estimatedStart, carArrivalTimes, transitInfo);

        } catch (Exception e) {
            System.err.println("!! 최종 AI 분석 중 심각한 오류 발생: " + e.getMessage());
            e.printStackTrace();
            rejectRequest(carRequest, AiDecision.DecisionResult.REJECT, "요청을 분석하는 중 시스템 오류가 발생했습니다.");
        }
    }

    /**
     * [공용 메서드] 주어진 요청 목록과 현재 차량 위치를 기반으로 최적의 카풀 경로와 시간표를 계산합니다.
     * @param allRequestsInCarpool 카풀에 포함된 모든 CarRequest 목록 (신규 확정자 포함)
     * @param vehicleLocation 차량의 현재 위치
     * @return 최적 경로 정보를 담은 CarpoolSolution 객체. 유효 경로가 없으면 Optional.empty()
     */
    public Optional<CarpoolSolution> calculateOptimalCarpoolSolution(List<CarRequest> allRequestsInCarpool, VehicleLocationDTO vehicleLocation) {
        List<CarRequest> originalRequests = allRequestsInCarpool.stream()
                .filter(req -> req.getStatus() == CarRequest.CarRequestStatus.CONFIRMED)
                .collect(Collectors.toList());

        CarRequest newRequest = allRequestsInCarpool.stream()
                .filter(req -> req.getStatus() != CarRequest.CarRequestStatus.CONFIRMED)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("카풀 계산에 신규 요청이 포함되지 않았습니다."));

        List<CarpoolPoint> pointsToVisit = new ArrayList<>();
        Map<CarRequest, CarpoolPoint> pickupMap = new HashMap<>();

        // ✅ 수정: 로직 개선. 모든 요청의 목적지를 한 번에 추가
        allRequestsInCarpool.forEach(req ->
                pointsToVisit.add(new CarpoolPoint(req, false, req.getDestinationLatitude(), req.getDestinationLongitude()))
        );
        // 신규 요청의 출발지만 별도로 추가
        CarpoolPoint newRequesterPickup = new CarpoolPoint(newRequest, true, newRequest.getRequesterLatitude(), newRequest.getRequesterLongitude());
        pointsToVisit.add(newRequesterPickup);
        pickupMap.put(newRequest, newRequesterPickup);

        // [로그 추가] 방문해야 할 모든 지점(Point) 목록 출력
        System.out.println("\n--- [2. 경유지 목록 생성] ---");
        pointsToVisit.forEach(p ->
                System.out.printf(">> 지점: ReqID:%d, %s, Lat:%.4f, Lon:%.4f\n",
                        p.getCarRequest().getId(), (p.isPickup() ? "픽업" : "목적지"), p.getLat(), p.getLon()));


        List<List<CarpoolPoint>> allValidPaths = new ArrayList<>();
        generateCarpoolPaths(new ArrayList<>(), pointsToVisit, pickupMap, allValidPaths);

        // [로그 추가] DFS로 생성된 모든 유효 경로 후보 출력
        System.out.println("\n--- [3. 유효 경로 조합 생성 (DFS 결과)] ---");
        System.out.println(">> 총 " + allValidPaths.size() + "개의 유효 경로 후보 생성됨.");
        int pathNum = 1;
        for (List<CarpoolPoint> path : allValidPaths) {
            String pathStr = path.stream()
                    .map(p -> "ReqID:" + p.getCarRequest().getId() + (p.isPickup() ? "(픽업)" : "(목적지)"))
                    .collect(Collectors.joining(" -> "));
            System.out.println("   - 경로 후보 " + pathNum++ + ": " + pathStr);
        }

        if (allValidPaths.isEmpty()) {
            return Optional.empty();
        }

        return findBestCarpoolPath(allValidPaths, vehicleLocation, originalRequests);
    }



    private String createOpenAiPrompt(User user, LocalDateTime estimatedStart, ArrivalTimes carInfo, TransitInfo transitInfo, WeatherInfo weatherInfo) {
        // 차량 픽업 대기 시간 계산
        long waitMinutes = Duration.between(estimatedStart, carInfo.timeAtRequester()).toMinutes();
        // 차량 탑승 시간 계산(차량 도착 시간 ~ 최종 목적지 도착 시간)
        long rideMinutes = Duration.between(carInfo.timeAtRequester(), carInfo.timeAtDestination()).toMinutes();

        // 대중교통 정보가 없는 경우를 위한 처리
        String transitDetails;
        if (transitInfo != null) {
            transitDetails = String.format(
                    "* **총 소요 시간**: 약 %d분\n" +
                            "* **총 도보 시간**: 약 %d분\n" +
                            "* **환승 횟수**: %d회\n",
                    transitInfo.totalMinutes(), transitInfo.walkMinutes(), transitInfo.transferCount()
            );
        } else {
            transitDetails = "* 이용 가능한 대중교통 정보를 찾을 수 없음.";
        }

        // 전체 프롬프트 조립
        return String.format(
                """
                가족 이동을 돕는 AI 비서다. 상황과 교통 정보를 바탕으로 자율주행차와 대중교통 중 더 적합한 선택을 추천해라.
                
                [현재 상황]
                요청 시간: %s
                사용자 상태: %s
                날씨: %s, 온도 %.1f°C(체감 %.1f°C)
                
                [자율주행차 이용 정보]
                픽업 대기 %d분, 탑승 %d분, 총 %d분. 문 앞에서 탑승, 환승·도보 없음.
                
                [대중교통 이용 정보]
                %s
                
                [규칙]
                - 종합적으로 판단해 JSON으로만 답변
                - decision: Vehicle 또는 Transit
                - reason: 1~2문장('~합니다.' 문체로 작성)
                
                출력 예시:
                {"decision":"Vehicle","reason":"비가 와서 도보가 불편해 자율주행차 추천합니다."}
                """,
                estimatedStart.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분")),
                user.getCondition(),
                weatherInfo.description(),
                weatherInfo.temperature(),
                weatherInfo.feelsLikeTemp(),
                waitMinutes,
                rideMinutes,
                waitMinutes + rideMinutes,
                transitDetails
        );

    }

    // AI 분석 결과를 AiDecision 엔티티에 저장하는 통일된 메서드
    private void saveDecision(CarRequest carRequest, AiDecision.DecisionResult result, String reason, LocalDateTime estimatedStart,
                              ArrivalTimes carInfo, TransitInfo transitInfo) {
        System.out.println("!!! Saving AiDecision -> reason: [" + reason + "]");
        long carTotalMinutes = 0;
        if (carInfo != null && estimatedStart != null) {
            carTotalMinutes = Duration.between(estimatedStart, carInfo.timeAtDestination()).toMinutes();
        }

        AiDecision decision = AiDecision.builder()
                .carRequest(carRequest)
                .decisionResult(result)
                .reason(reason)
                .estimatedPickupTime(carInfo != null ? carInfo.timeAtRequester() : null)
                .estimatedDestinationTime(carInfo != null ? carInfo.timeAtDestination() : null)
                .carTotalTime((int) carTotalMinutes)
                .transitTotalTime(transitInfo != null ? transitInfo.totalMinutes() : null)
                .build();
        aiDecisionRepository.save(decision);

    }

    // 규칙 기반 거절 메서드도 saveDecision을 호출하도록 수정 가능 (단, carInfo, transitInfo가 없을 수 있음)
    private void rejectRequest(CarRequest carRequest, AiDecision.DecisionResult result, String reason) {
        carRequest.setStatus(CarRequest.CarRequestStatus.REJECTED);
        // 이 경우에는 시간 정보가 없으므로 null로 저장됨
        saveDecision(carRequest, result, reason, null, null, null);
    }

}

//                """
//                너는 우리 가족의 이동을 돕는 AI 비서야. 사용자의 현재 상황과 교통 정보를 바탕으로 자율주행차 이용과 대중교통 이용 중 더 나은 선택을 추천해줘.
//
//                [현재 상황]
//                * **요청 시간**: %s
//                * **사용자 상태**: "%s"
//                * **현재 날씨**: %s, 온도: %.1f°C (체감 온도: %.1f°C)
//
//                [선택지 1: 자율주행차 이용]
//                * **픽업 대기 시간**: 약 %d분
//                * **차량 탑승 시간**: 약 %d분
//                * **총 소요 시간**: 약 %d분
//                * **특징**: 문 앞에서 바로 탑승하여 목적지까지 편안하게 이동 가능. 환승이나 도보 없음.
//
//                [선택지 2: 대중교통 이용]
//                %s
//
//                [너의 임무]
//                - 사용자의 컨디션과 날씨, 이동 시간, 편의성을 종합적으로 고려해 더 적합한 선택지를 추천해라.
//                - 반드시 JSON 형식으로만 답변해라.
//                - "decision"은 "Vehicle" 또는 "Transit" 중 하나여야 한다.
//                - "reason"은 1~2문장 이내로 간단히 작성해라.
//
//                [출력 예시]
//                ```json
//                {
//                  "decision": "Vehicle",
//                  "reason": "비가 오고 도보 시간이 길어 자율주행차가 더 적합합니다."
//                }
//                ```
//                """,
//                """
//                너는 우리 가족의 이동을 돕는 AI 비서야. 사용자의 현재 상황과 교통 정보를 바탕으로 자율주행차 이용과 대중교통 이용 중 더 나은 선택을 추천해줘.
//
//                [현재 상황]
//                * **요청 시간**: %s
//                * **사용자 상태**: "%s"
//                * **현재 날씨**: %s, 온도: %.1f°C (체감 온도: %.1f°C)
//
//
//                [선택지 1: 자율주행차 이용]
//                * **픽업 대기 시간**: 약 %d분
//                * **차량 탑승 시간**: 약 %d분
//                * **총 소요 시간**: 약 %d분
//                * **특징**: 문 앞에서 바로 탑승하여 목적지까지 편안하게 이동 가능. 환승이나 도보 없음.
//
//                [선택지 2: 대중교통 이용]
//                %s
//
//                [너의 임무]
//                위의 모든 정보를 종합적으로 고려해서, 사용자에게 더 적합한 선택지를 추천하고 그 이유를 설명해줘. 답변은 반드시 아래의 JSON 형식으로만 제공해줘.
//                ```json
//                {
//                  "decision": "Vehicle" | "Transit",
//                  "reason": "추천 이유를 여기에 설명"
//                }
//                ```
//                """,

//    @Async
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public void processDecision(Long carRequestId) {
//        System.out.println("\n===== [AsyncDecisionService] AI 분석 시작 (ID: " + carRequestId + ") =====");
//        CarRequest carRequest = carRequestRepository.findById(carRequestId)
//                .orElseThrow(() -> new CustomException(ErrorCode.CARREQUEST_NOT_FOUND));
//
//        try {
//
//            User user = carRequest.getUser();
//            FamilyGroup familyGroup = user.getFamilyGroup();
//            Vehicle vehicle = familyGroup.getVehicle();
//            Long groupId = familyGroup.getId();
//            Long vehicleId = vehicle.getId();
//            Long userId = user.getId();
//
//            // Redis에서 차량의 실시간 위치/상태 정보 조회
//            Optional<VehicleLocationDTO> vehicleLocationOptional = locationCacheService.getVehicleLocation(groupId, vehicleId);
//
//            // Redis에서 요청자의 실시간 위치 정보 조회
//            Optional<LocationDTO> userLocationOptional = locationCacheService.getUserLocation(groupId, userId);
//
//            // 차량 또는 요청자의 위치 정보가 없는 경우 요청 거절
//            if (vehicleLocationOptional.isEmpty()) {
//                System.out.println(">> [규칙 거절] 차량의 실시간 위치 정보를 찾을 수 없음 (Redis)");
//                rejectRequest(carRequest, AiDecision.DecisionResult.REJECT, "차량의 현재 위치를 확인할 수 없습니다.");
//                return;
//            }
//
//            if (userLocationOptional.isEmpty()) {
//                System.out.println(">> [규칙 거절] 요청자의 실시간 위치 정보를 찾을 수 없음 (Redis)");
//                rejectRequest(carRequest, AiDecision.DecisionResult.REJECT, "요청자의 현재 위치를 확인할 수 없습니다.");
//                return;
//            }
//
//            // 실시간 정보 DTO 가져오기
//            VehicleLocationDTO realTimeVehicleLocation  = vehicleLocationOptional.get();
//            LocationDTO realTimeUserLocation = userLocationOptional.get(); // ✅ 요청자 위치 DTO 가져오기
//
//            // 규칙 1: Redis의 실시간 차량 상태 확인 -> 나중에 카풀 확인으로 바꿀 거임
//            if(realTimeVehicleLocation.getStatus() != Vehicle.VehicleStatus.Idle) {
//                System.out.println(">> [규칙 거절] 차량이 유휴 상태가 아님 (실시간): " + realTimeVehicleLocation.getStatus());
//                rejectRequest(carRequest, AiDecision.DecisionResult.REJECT, "차량을 현재 다른 가족 구성원이 이용 중입니다."); // 사실 충전 중일 때도 있음...
//                return;
//            }
//
//            // Redis에서 가져온 차량의 실시간 위치 정보
//            double vehicleLat = realTimeVehicleLocation.getLatitude();
//            double vehicleLon = realTimeVehicleLocation.getLongitude();
//
//            // Redis에서 가져온 요청자의 실시간 위치 정보
//            double userLat = realTimeUserLocation.getLatitude();
//            double userLon = realTimeUserLocation.getLongitude();
//
//            // CarRequest에서 목적지 위치 정보 가져오기
//            double destLat = carRequest.getDestinationLatitude();
//            double destLon = carRequest.getDestinationLongitude();
//
//            System.out.printf(">> [정보] 사용자 위치: (%.4f, %.4f), 차량 위치: (%.4f, %.4f)\n", userLat, userLon, vehicleLat, vehicleLon);
//
//            // TMAP 차량 운행 시 정보 조회(다중 경유지 API)
//            // ==============================================================================
//
//            // 1. TMAP API 호출에 필요한 Coordinate 객체 3개 생성
//            Coordinate vehicleCoord = new Coordinate(String.valueOf(vehicleLat), String.valueOf(vehicleLon));
//            Coordinate requesterCoord = new Coordinate(String.valueOf(userLat), String.valueOf(userLon));
//            Coordinate destinationCoord = new Coordinate(String.valueOf(destLat), String.valueOf(destLon));
//
//            ArrivalTimes arrivalTimes;
//            try {
//                // 2. TmapService를 호출하여 예상 도착 시간 정보 조회
//                System.out.println(">> [TMAP] 경로 및 도착 시간 조회 시작...");
//                arrivalTimes = tmapService.getArrivalTimes(vehicleCoord, requesterCoord, destinationCoord);
//
//                // arrivalTimes가 null인 경우 (경로 탐색 실패) 처리
//                if (arrivalTimes == null) {
//                    rejectRequest(carRequest, AiDecision.DecisionResult.REJECT, "차량 이동 경로를 찾을 수 없습니다.");
//                    return;
//                }
//            } catch (RuntimeException e) {
//
//                // ✅ 어떤 에러가 발생했는지 정확히 확인하기 위해 로그를 추가합니다.
//                System.err.println("!!! TMAP API 호출 중 심각한 오류 발생 !!!");
//                e.printStackTrace(); // 👈 이 코드가 에러의 전체 내용을 콘솔에 출력해 줍니다.
//
//                // TmapClient에서 API 호출 실패시 RuntimeException이 발생하므로 여기서 처리
//                rejectRequest(carRequest, AiDecision.DecisionResult.REJECT, "이동 경로를 계산하는 중 오류가 발생했습니다.");
//                return;
//
//            }
//
//            // 3. 결과 확인 및 로깅
//            LocalDateTime timeAtRequester = arrivalTimes.timeAtRequester();
//            LocalDateTime timeAtDestination = arrivalTimes.timeAtDestination();
//
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//            System.out.println(">> [TMAP 결과] 요청자 위치 도착 예상 시간: " + timeAtRequester.format(formatter));
//            System.out.println(">> [TMAP 결과] 최종 목적지 도착 예상 시간: " + timeAtDestination.format(formatter));
//
//            // ==============================================================================
//
//
//            // TMAP 대중교통 이용 시 정보 조회
//            TransitInfo transitInfo = transitService.getTransitInfo(
//                    String.valueOf(userLon),
//                    String.valueOf(userLat),
//                    String.valueOf(destLon),
//                    String.valueOf(destLat)
//            );
//
//            if (transitInfo == null){
//                System.out.println("조회된 대중교통 경로가 없어 다른 로직을 수행합니다.");
//            }
//
//            // 규칙 2: 예약 시간 겹침 확인(겹치면 Reject -> 무조건 예약자가 사전에 설정한 도착 시간 맞출 수 없음 + 짧은 시간 내에 예약자에게 카풀 요청 보내고 수락 받아야 하는데 시간상 거절할 확률 높다고 판단)
//            LocalDateTime estimatedStart = LocalDateTime.now(); // 현재 시점부터 기준
//            List<Reservation> overlaps = reservationRepository.findOverlappingReservationsForVehicle(
//                    vehicle.getId(), estimatedStart, timeAtDestination
//            );
//
//            if (!overlaps.isEmpty()) {
//                System.out.println(">> [규칙 거절] 기존 예약과 시간 겹침 발생");
//                rejectRequest(carRequest, AiDecision.DecisionResult.REJECT, "해당 시간대에 다른 가족의 예약이 이미 존재합니다.");
//                return;
//            }
//
//            // 날씨 정보 가져오기
//            WeatherInfo weatherInfo = weatherService.getCurrentWeatherInfo(
//                    String.valueOf(userLat),
//                    String.valueOf(userLon)
//            );
//
//            System.out.println(">> [날씨 정보] 현재 날씨: " + weatherInfo.description() +
//                    ", 온도: " + weatherInfo.temperature() + "°C");
//
//
//
//            // OpenAI를 위한 프롬프트 엔지니어링 및 API 호출
//            System.out.println(">> [OpenAI] 의사결정 프롬프트 생성 및 API 호출 시작...");
//
//            String prompt = createOpenAiPrompt(user, estimatedStart, arrivalTimes,transitInfo, weatherInfo);
//            OpenAiDecisionResponse aiResponse = openAiClient.getDecision(prompt);
//
//
//            // AI 응답에 따른 최종 결정 및 '모든 정보' 저장
//            // AI 추천과 상관없이, 계산된 차량 예상 시간 정보를 CarRequest에 먼저 저장합니다.
//            carRequest.setEstimatedTimes(arrivalTimes.timeAtRequester(), arrivalTimes.timeAtDestination());
//
//            // 2. CarRequest의 상태를 '분석 완료'로 변경하여 사용자 확인을 기다립니다.
//            carRequest.setStatus(CarRequest.CarRequestStatus.ANALYSIS_COMPLETE);
//
//            // 3. AI의 결정(VEHICLE 또는 PUBLIC_TRANSPORT)에 따라 DecisionResult를 결정합니다.
//            AiDecision.DecisionResult result = aiResponse.decision().equalsIgnoreCase("Vehicle")
//                    ? AiDecision.DecisionResult.VEHICLE // AI는 차량 이용을 '추천(Approve)'
//                    : AiDecision.DecisionResult.PUBLIC_TRANSPORT; // AI는 대중교통을 '추천(Suggest)'
//
//            // 4. 모든 분석 결과를 AiDecision에 저장합니다.
//            saveDecision(carRequest, result, aiResponse.reason(), estimatedStart, arrivalTimes, transitInfo);
//
////            if (aiResponse.decision().equalsIgnoreCase("Vehicle")) {
////                // 1. CarRequest 상태를 '승인'으로 변경
////                carRequest.approve(arrivalTimes.timeAtRequester(), arrivalTimes.timeAtDestination());
////
////                // 2. 모든 분석 결과를 AiDecision에 저장
////                saveDecision(carRequest, AiDecision.DecisionResult.VEHICLE, aiResponse.reason(), estimatedStart, arrivalTimes, transitInfo);
////            } else {
////                // 1. CarRequest 상태를 '거절'로 변경 (대중교통 추천도 일단 거절 상태)
////                carRequest.setStatus(CarRequest.CarRequestStatus.REJECTED);
////
////                // 2. 모든 분석 결과를 AiDecision에 저장
////                saveDecision(carRequest, AiDecision.DecisionResult.PUBLIC_TRANSPORT, aiResponse.reason(), estimatedStart, arrivalTimes, transitInfo);
////            }
//
//
//
//
//        } catch (Exception e) {
//            // 예외 발생 시 요청 상태를 REJECTED로 변경하는 등 예외 처리
//            System.err.println("AI 분석 중 오류 발생: " + e.getMessage());
//            rejectRequest(carRequest, AiDecision.DecisionResult.REJECT, "분석 중 시스템 오류가 발생했습니다.");
//        }
//    }
