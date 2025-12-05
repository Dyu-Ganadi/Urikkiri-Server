package com.example.urikkiriserver.component;

import lombok.NonNull;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class CustomHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        // HTTP 요청이 Security 필터를 통과한 후, Principal을 세션에 복사
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            // Principal 객체 복사 (Principal을 사용하려면)
            attributes.put("userPrincipal", SecurityContextHolder.getContext().getAuthentication().getPrincipal());

            // 또는 세션의 Principal로 직접 설정하려면 DefaultHandshakeHandler를 사용해야 함
        }
        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, Exception exception) {
    }
}
