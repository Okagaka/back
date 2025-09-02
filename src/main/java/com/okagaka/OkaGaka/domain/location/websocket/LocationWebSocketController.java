package com.okagaka.OkaGaka.domain.location.websocket;

import com.okagaka.OkaGaka.common.exception.CustomException;
import com.okagaka.OkaGaka.common.exception.ErrorCode;
import com.okagaka.OkaGaka.domain.location.dto.LocationDTO;
import com.okagaka.OkaGaka.domain.location.service.LocationCacheService;
import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;


import java.time.LocalDateTime;

@Controller
public class LocationWebSocketController {

    @Autowired
    private LocationCacheService cacheService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserRepository userRepository;

    @MessageMapping("/location/update")
    public void updateLocation(@Payload LocationDTO dto, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Unauthorized: Principal is null");
        }

        // 1. userId가 비어있으면 Principal에서 가져오기
        Long userId = Long.parseLong(principal.getName());
        dto.setUserId(userId);

        // 2. groupId 자동 채움
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getFamilyGroup() == null) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND);
        }

        dto.setGroupId(user.getFamilyGroup().getId());

        dto.setTimestamp(LocalDateTime.now());

        // 1. Redis에 저장
        cacheService.saveLocation(dto);

        // 2. 그룹 채널로 브로드캐스트
        messagingTemplate.convertAndSend("/topic/group/" + dto.getGroupId(), dto);
//        cacheService.saveLocation(dto); // 중복이어서 삭제
    }

}
