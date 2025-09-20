package com.okagaka.OkaGaka.domain.tmap.dto;

import java.time.LocalDateTime;

public record ArrivalTimes(
        LocalDateTime timeAtRequester,
        LocalDateTime timeAtDestination
) {}
