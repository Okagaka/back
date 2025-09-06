package com.okagaka.OkaGaka.domain.location.websocket;

import com.okagaka.OkaGaka.common.exception.CustomException;
import com.okagaka.OkaGaka.common.exception.ErrorCode;
import com.okagaka.OkaGaka.domain.location.dto.LocationDTO;
import com.okagaka.OkaGaka.domain.location.service.LocationCacheService;
import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.domain.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
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
    public void updateLocation(@Payload LocationDTO dto, StompHeaderAccessor accessor) {
        System.out.println("✅ @MessageMapping 호출됨! 메시지 수신: " + dto.toString());

        // accessor.getUser() 대신 세션(SessionAttributes)에서 직접 인증 정보를 꺼냅니다.
        // 이 정보는 스레드가 변경되어도 유지됩니다.
        Authentication authentication = (Authentication) accessor.getSessionAttributes().get("user");

        if (authentication == null) {
            // 만약 세션에도 없다면, 그 때 에러를 발생시킵니다.
            throw new IllegalArgumentException("Unauthorized: Authentication not found in session");
        }

        System.out.println("✅ 세션에서 직접 꺼낸 Authentication.getName(): " + authentication.getName());

        // --- 이하 로직은 동일 ---
        Long userId = Long.parseLong(authentication.getName());
        dto.setUserId(userId);

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
    }
}
