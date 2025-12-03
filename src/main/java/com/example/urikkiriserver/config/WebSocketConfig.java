package com.example.urikkiriserver.config;

import com.example.urikkiriserver.component.CustomHandshakeInterceptor;
import com.example.urikkiriserver.component.WebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@RequiredArgsConstructor
@EnableWebSocket
@Configuration
public class WebSocketConfig implements WebSocketConfigurer {
    private final WebSocketHandler webSocketHandler;
    private final CustomHandshakeInterceptor customHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 웹소켓 엔드포인트: /ws
        // 바꿀 수 있음
        registry.addHandler(webSocketHandler, "/ws").setAllowedOriginPatterns("*").addInterceptors(customHandshakeInterceptor);
    }
}