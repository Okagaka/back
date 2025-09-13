package com.okagaka.OkaGaka.common.config;

import com.okagaka.OkaGaka.common.security.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            try {
                String token = accessor.getFirstNativeHeader("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                    if (jwtTokenProvider.validateToken(token)) {
                        Long userId = jwtTokenProvider.getUserId(token);
                        Authentication authentication = new UsernamePasswordAuthenticationToken(
//                                userId.toString(),
                                String.valueOf(userId),
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                        accessor.setUser(authentication);
                        accessor.getSessionAttributes().put("user", authentication);
                        System.out.println("✅ WebSocket JWT 인증 성공: userId=" + userId);
                    }
                }
            } catch (JwtException e) {
                System.err.println("❌ WebSocket JWT Error: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("❌ WebSocket Interceptor Error: " + e.getMessage());
                e.printStackTrace();
            }
        } else if (accessor.getUser() == null && accessor.getSessionAttributes() != null) {
            Authentication authentication = (Authentication) accessor.getSessionAttributes().get("user");
            if (authentication != null) {
                accessor.setUser(authentication);
            }
        }
        System.out.println("✅ preSend: command=" + accessor.getCommand() + ", principal=" + (accessor.getUser() != null ? accessor.getUser().getName() : "NULL"));
        return message;
    }
}
