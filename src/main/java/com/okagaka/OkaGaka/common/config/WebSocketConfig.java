package com.okagaka.OkaGaka.common.config;


import io.jsonwebtoken.JwtException;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.okagaka.OkaGaka.common.security.JwtTokenProvider;

import java.util.Collections;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer{

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // 구독 경로(prefix) - 클라이언트가 구독할 수 있는 대상 경로(/topic으로 시작하는 경로에 브로커가 메시지를 보낼 수 있도록 설정)
        config.setApplicationDestinationPrefixes("/app"); // 클라이언트 전송 경로(클라이언트가 서버로 메시지를 보낼 때 사용하는 prefix)
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-location").setAllowedOrigins("*"); // .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                // STOMP CONNECT 명령이 도착했을 떄만 인증 로직 수행
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authToken = accessor.getFirstNativeHeader("Authorization");

                    if (authToken != null && authToken.startsWith("Bearer")) {
                        String jwt = authToken.substring(7);

                        try{
                            // 1. JWT 토큰 유효성 검사
                            if (jwtTokenProvider.validateToken(jwt)){
                                // 2. 토큰에서 사용자 ID 추출
                                Long userId = jwtTokenProvider.getUserId(jwt);
                                // 3. 사용자 인증 정보(Principal) 생성 및 세션에 설정
                                // Spring Security의 Authentication 객체를 생성하여 세션에 연결합니다.
                                // 여기서는 간단하게 userId를 Principal로 사용하며,
                                // 'ROLE_USER'라는 기본 권한을 부여했습니다.
                                // 실제 서비스에서는 UserDetailsService 등을 통해 UserDetails를 로드하고
                                // 해당 사용자의 실제 권한 목록을 가져와 설정하는 것이 일반적입니다.
                                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                        userId,
                                        null,
                                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                                );
                                accessor.setUser(authentication); // STOMP 세션에 Principal 설정

                                // 선택 사항: SecurityContextHolder에도 설정 (필요한 경우)
                                // SecurityContextHolder.getContext().setAuthentication(authentication);

                            } else {
                                // 4. 토큰이 유효하지 않은 경우: 연결 거부 또는 예외 발생
                                // 예외를 던지면 클라이언트의 WebSocket 연결이 끊어집니다.
                                System.err.println("Invalid JWT token received for Websocket connection.");
                                throw new IllegalArgumentException("Invalid JWT token");

                            }
                        } catch (JwtException | IllegalArgumentException e) {
                            // 토큰 파싱 실패, 만료 등 JWT 관련 예외 처리
                            System.err.println("Error validating JWT token for WebSocket: " + e.getMessage());
                            throw new IllegalArgumentException("Authentication failed: " + e.getMessage());
                        }
                    } else {
                        // Authorization 헤더가 없거나 형식이 잘못된 경우
                        System.err.println("Authorization header missing or malformed for WebSocket connection.");
                        throw new IllegalArgumentException("Authorization header is missing or malformed.");
                    }
                }
                // 연결 끊김(DISCONNECT) 명령 처리 (선택 사항)
                // 클라이언트가 연결을 끊을 때 특정 로직을 수행해야 한다면 여기에 추가
                else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                    // 예: 연결이 끊긴 사용자의 현재 위치 캐시를 제거하거나 로그 기록
                    if (accessor.getUser() != null) {
                        System.out.println("User " + accessor.getUser().getName() + " disconnected from WebSocket.");
                        // cacheService.removeLocationByUserId(Long.parseLong(accessor.getUser().getName()));
                    }
                }

                return message; // 처리된 메시지를 다음 인터셉터 또는 메시지 핸들러로 전달
            }
        });
    }

}
