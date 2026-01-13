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
        String token = null;

        // 1. 쿼리 파라미터에서 토큰 추출 (브라우저 환경용)
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    token = param.substring(6); // "token=" 이후의 값
                    break;
                }
            }
        }

        // 2. Authorization 헤더에서 토큰 추출 (네이티브 앱 등에서 사용 가능)
        if (token == null) {
            List<String> authHeaders = request.getHeaders().get("Authorization");
            if (authHeaders != null && !authHeaders.isEmpty()) {
                token = authHeaders.get(0);
                if (token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }
            }
        }

        // 토큰이 없으면 연결 거부
        if (token == null || token.isEmpty()) {
            return false;
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