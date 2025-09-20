package com.okagaka.OkaGaka.domain.carrequest.service;

import com.okagaka.OkaGaka.common.exception.CustomException;
import com.okagaka.OkaGaka.common.exception.ErrorCode;
import com.okagaka.OkaGaka.common.external.tmap.Coordinate;
import com.okagaka.OkaGaka.common.external.tmap.TmapGeocodingClient;
import com.okagaka.OkaGaka.domain.aidecision.dto.AiDecisionResponse;
import com.okagaka.OkaGaka.domain.aidecision.entity.AiDecision;
import com.okagaka.OkaGaka.domain.aidecision.repository.AiDecisionRepository;
//import com.okagaka.OkaGaka.domain.aidecision.service.AsyncDecisionFacade;
import com.okagaka.OkaGaka.domain.aidecision.service.AsyncDecisionService;
import com.okagaka.OkaGaka.domain.carrequest.dto.CarRequestDto;
import com.okagaka.OkaGaka.domain.carrequest.dto.CarRequestResponse;
import com.okagaka.OkaGaka.domain.carrequest.entity.CarRequest;
import com.okagaka.OkaGaka.domain.carrequest.repository.CarRequestRepository;
import com.okagaka.OkaGaka.domain.location.dto.LocationDTO;
import com.okagaka.OkaGaka.domain.location.service.LocationCacheService;
import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class CarRequestService {

    private final CarRequestRepository carRequestRepository;
    private final UserRepository userRepository;
    private final TmapGeocodingClient tmapGeocodingClient;
    private final AiDecisionRepository aiDecisionRepository;
    private final AsyncDecisionService asyncDecisionService;
    private final LocationCacheService locationCacheService;

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


        // 2. Redis에서 요청자의 실시간 위치 정보 조회
        Long groupId = user.getFamilyGroup().getId();
        LocationDTO userLocation = locationCacheService.getUserLocation(groupId, userId)
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
                asyncDecisionService.processDecision(savedRequest.getId());
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

        return AiDecisionResponse.from(decision);
    }

}
