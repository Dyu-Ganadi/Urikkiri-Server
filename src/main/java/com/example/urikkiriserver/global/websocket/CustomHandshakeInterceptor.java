package com.example.urikkiriserver.global.websocket;

import com.example.urikkiriserver.global.security.auth.AuthDetails;
import com.example.urikkiriserver.global.security.jwt.JwtProvider;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        // Authorization 헤더에서 토큰 추출
        List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            return false;
        }

        String token = authHeaders.get(0);
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            // JWT 토큰 검증 및 인증 정보 추출
            Authentication authentication = jwtProvider.authentication(token);

            // AuthDetails에서 실제 User 객체 추출
            if (authentication.getPrincipal() instanceof AuthDetails authDetails) {
                attributes.put("userPrincipal", authDetails.getUser());
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                              @NonNull WebSocketHandler wsHandler, Exception exception) {
    }
}
