package com.okagaka.OkaGaka.domain.tmap.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;


@Getter
@Setter
@AllArgsConstructor
public class CarpoolCheckResult {
    private Map<Long, LocalDateTime> updatedDepartureTimes; // 예약Id -> 새로운 출발시간(epoch sec)
    private int newReservationTravelTimeSec;       // 새 예약 소요시간
}
