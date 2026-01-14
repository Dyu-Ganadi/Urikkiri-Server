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
        ClientType clientType = ClientType.LOBBY; // 기본값은 로비

        // 1. 쿼리 파라미터에서 토큰과 클라이언트 타입 추출
        String query = request.getURI().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    token = param.substring(6);
                } else if (param.startsWith("clientType=")) {
                    String typeValue = param.substring(11);
                    try {
                        clientType = ClientType.valueOf(typeValue.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // 잘못된 clientType이면 기본값(LOBBY) 사용
//                        log.warn("Invalid clientType: {}, using default LOBBY", typeValue);
                    }
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
                attributes.put("clientType", clientType); // 클라이언트 타입 추가
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