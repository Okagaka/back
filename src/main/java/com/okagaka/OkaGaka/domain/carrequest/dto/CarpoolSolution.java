package com.okagaka.OkaGaka.domain.carrequest.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class CarpoolSolution {

    private final List<CarpoolPoint> path;
    private final Map<CarpoolPoint, LocalDateTime> timetable;
    private final long totalDurationMinutes;
}
