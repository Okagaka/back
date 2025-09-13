package com.okagaka.OkaGaka.domain.tmap.dto;

import com.okagaka.OkaGaka.domain.reservation.service.CarpoolService;
import com.okagaka.OkaGaka.domain.reservation.entity.Reservation;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.List;

import java.time.LocalDateTime;
import java.util.Map;



@Getter
@Setter
@ToString
@AllArgsConstructor
public class CarpoolCheckResult {
//    private Map<Long, LocalDateTime> updatedDepartureTimes; // 예약Id -> 새로운 출발시간(epoch sec)
//    private int newReservationTravelTimeSec;       // 새 예약 소요시간

//    // 최적의 운행 순서 (경유지 목록)
//    private final List<CarpoolService.Point> optimalPath;
//
//    // 각 예약별로 계산된 픽업 시간
//    private final Map<Reservation, LocalDateTime> departureTimes;
//
//    // 최적 경로의 총 운행 시간 (초)
//    private final long totalTravelTimeSeconds;

    /**
     * 최종 선택된 최적 경로 (디버깅 또는 상세 정보 표기용)
     */
    private final List<CarpoolService.Point> optimalPath;

    /**
     * 새로 요청한 예약의 계산된 출발/도착 시간
     */
    private final LocalDateTime newReservationDepartureTime;

    private final LocalDateTime newReservationArrivalTime;

    /**
     * 새로 요청한 예약의 순수 이동 시간 (초)
     * (본인의 픽업 지점부터 하차 지점까지의 시간)
     */
    private final int newReservationTravelTimeSec;

    /**
     * 기존 예약자들의 변경될 출발 시간 제안 맵
     * Key: 기존 Reservation의 ID, Value: 제안되는 새로운 출발 시간
     */
    private final Map<Long, LocalDateTime> updatedDepartureTimes; // 기존 예약자들의 변경될 출발 시간

    private final Map<Long, LocalDateTime> updatedArrivalTimes;   // 기존 예약자들의 변경될 도착 시간

    // Key: 예약 ID, Value: 변경된 소요 시간
    private final Map<Long, Integer> updatedTravelTimesSec;

    /**
     * 최적 경로의 총 운행 시간 (초)
     */
    private final long totalTravelTimeSeconds;
}
