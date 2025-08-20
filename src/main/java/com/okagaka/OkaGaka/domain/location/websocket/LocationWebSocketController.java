package com.okagaka.OkaGaka.domain.location.websocket;

import com.okagaka.OkaGaka.domain.location.dto.LocationDTO;
import com.okagaka.OkaGaka.domain.location.service.LocationCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class LocationWebSocketController {

    @Autowired
    private LocationCacheService cacheService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/location/update")
    public void updateLocation(@Payload LocationDTO dto) {
        dto.setTimestamp(LocalDateTime.now());

        // 1. Redis에 저장
        cacheService.saveLocation(dto);

        // 2. 그룹 채널로 브로드캐스트
        messagingTemplate.convertAndSend("/topic/group/" + dto.getGroupId(), dto);
//        cacheService.saveLocation(dto); // 중복이어서 삭제
    }

}
