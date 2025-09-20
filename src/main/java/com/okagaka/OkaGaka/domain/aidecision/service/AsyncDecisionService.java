package com.okagaka.OkaGaka.domain.aidecision.service;

import com.okagaka.OkaGaka.common.exception.CustomException;
import com.okagaka.OkaGaka.common.exception.ErrorCode;
import com.okagaka.OkaGaka.common.external.openai.OpenAiClient;
import com.okagaka.OkaGaka.common.external.tmap.Coordinate;
import com.okagaka.OkaGaka.domain.aidecision.dto.OpenAiDecisionResponse;
import com.okagaka.OkaGaka.domain.aidecision.dto.WeatherInfo;
import com.okagaka.OkaGaka.domain.aidecision.entity.AiDecision;
import com.okagaka.OkaGaka.domain.aidecision.repository.AiDecisionRepository;
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
import java.util.List;
import java.util.Optional;

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

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processDecision(Long carRequestId) {
        System.out.println("\n===== [AsyncDecisionService] AI 분석 시작 (ID: " + carRequestId + ") =====");
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
            VehicleLocationDTO realTimeVehicleLocation  = vehicleLocationOptional.get();
            LocationDTO realTimeUserLocation = userLocationOptional.get(); // ✅ 요청자 위치 DTO 가져오기

            // 규칙 1: Redis의 실시간 차량 상태 확인 -> 나중에 카풀 확인으로 바꿀 거임
            if(realTimeVehicleLocation.getStatus() != Vehicle.VehicleStatus.Idle) {
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

            // ==============================================================================


            // TMAP 대중교통 이용 시 정보 조회
            TransitInfo transitInfo = transitService.getTransitInfo(
                    String.valueOf(userLon),
                    String.valueOf(userLat),
                    String.valueOf(destLon),
                    String.valueOf(destLat)
            );

            if (transitInfo == null){
                System.out.println("조회된 대중교통 경로가 없어 다른 로직을 수행합니다.");
            }

            // 규칙 2: 예약 시간 겹침 확인(겹치면 Reject -> 무조건 예약자가 사저넹 설정한 도착 시간 맞출 수 없음 + 짧은 시간 내에 예약자에게 카풀 요청 보내고 수락 받아야 하는데 시간상 거절할 확률 높다고 판단)
            LocalDateTime estimatedStart = LocalDateTime.now(); // 현재 시점부터 기준
            List<Reservation> overlaps = reservationRepository.findOverlappingReservationsForVehicle(
                    vehicle.getId(), estimatedStart, timeAtDestination
            );

            if (!overlaps.isEmpty()) {
                System.out.println(">> [규칙 거절] 기존 예약과 시간 겹침 발생");
                rejectRequest(carRequest, AiDecision.DecisionResult.REJECT, "해당 시간대에 다른 가족의 예약이 이미 존재합니다.");
                return;
            }

            // 날씨 정보 가져오기
            WeatherInfo weatherInfo = weatherService.getCurrentWeatherInfo(
                    String.valueOf(userLat),
                    String.valueOf(userLon)
            );

            System.out.println(">> [날씨 정보] 현재 날씨: " + weatherInfo.description() +
                    ", 온도: " + weatherInfo.temperature() + "°C");



            // OpenAI를 위한 프롬프트 엔지니어링 및 API 호출
            System.out.println(">> [OpenAI] 의사결정 프롬프트 생성 및 API 호출 시작...");

            String prompt = createOpenAiPrompt(user, estimatedStart, arrivalTimes,transitInfo, weatherInfo);
            OpenAiDecisionResponse aiResponse = openAiClient.getDecision(prompt);


            // AI 응답에 따른 최종 결정 및 '모든 정보' 저장
            if (aiResponse.decision().equalsIgnoreCase("Vehicle")) {
                // 1. CarRequest 상태를 '승인'으로 변경
                carRequest.approve(arrivalTimes.timeAtRequester(), arrivalTimes.timeAtDestination());

                // 2. 모든 분석 결과를 AiDecision에 저장
                saveDecision(carRequest, AiDecision.DecisionResult.VEHICLE, aiResponse.reason(), estimatedStart, arrivalTimes, transitInfo);
            } else {
                // 1. CarRequest 상태를 '거절'로 변경 (대중교통 추천도 일단 거절 상태)
                carRequest.setStatus(CarRequest.CarRequestStatus.REJECTED);

                // 2. 모든 분석 결과를 AiDecision에 저장
                saveDecision(carRequest, AiDecision.DecisionResult.PUBLIC_TRANSPORT, aiResponse.reason(), estimatedStart, arrivalTimes, transitInfo);
            }


        } catch (Exception e) {
            // 예외 발생 시 요청 상태를 REJECTED로 변경하는 등 예외 처리
            System.err.println("AI 분석 중 오류 발생: " + e.getMessage());
            rejectRequest(carRequest, AiDecision.DecisionResult.REJECT, "분석 중 시스템 오류가 발생했습니다.");
        }
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
