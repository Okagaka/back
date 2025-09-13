package com.okagaka.OkaGaka.common.config;


import com.okagaka.OkaGaka.common.security.CustomUserDetailsService;
import io.jsonwebtoken.JwtException;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.okagaka.OkaGaka.common.security.JwtTokenProvider;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.List;

import java.util.Collections;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer{

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
//    private final CustomUserDetailsService userDetailsService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue"); // 구독 경로(prefix) - 클라이언트가 구독할 수 있는 대상 경로(/topic으로 시작하는 경로에 브로커가 메시지를 보낼 수 있도록 설정)
        config.setApplicationDestinationPrefixes("/app"); // 클라이언트 전송 경로(클라이언트가 서버로 메시지를 보낼 때 사용하는 prefix)
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-location").setAllowedOrigins("*"); //.withSockJS();

    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                // 1. CONNECT 요청: JWT 인증 후 세션에 Principal 저장
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);
                        if (jwtTokenProvider.validateToken(token)) {
                            Long userId = jwtTokenProvider.getUserId(token);
                            Authentication authentication = new UsernamePasswordAuthenticationToken(
                                    userId.toString(), null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                            );

                            // 현재 메시지의 Principal 설정
                            accessor.setUser(authentication);

                            // WebSocket 세션 속성에 Principal 저장 (가장 중요!)
                            // 이후의 SUBSCRIBE, SEND 요청에서 이 값을 사용하게 됩니다.
                            accessor.getSessionAttributes().put("user", authentication);

                            System.out.println("✅ WebSocket JWT 인증 성공: userId=" + userId);
                        }
                    }
                }
                // 2. 다른 모든 요청(SUBSCRIBE, SEND 등): 세션에서 Principal을 꺼내 현재 메시지에 설정
                else if (accessor.getUser() == null && accessor.getSessionAttributes() != null) {
                    // 세션에서 저장해 둔 인증 정보 가져오기
                    Authentication authentication = (Authentication) accessor.getSessionAttributes().get("user");

                    // 현재 메시지의 Principal로 설정
                    if (authentication != null) {
                        accessor.setUser(authentication);
                    }
                }

                System.out.println("✅ preSend: command=" + accessor.getCommand() +
                        ", principal=" + (accessor.getUser() != null ? accessor.getUser().getName() : "NULL"));

                return message;
            }
        });
    }



}
