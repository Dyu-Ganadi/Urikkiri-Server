package com.example.urikkiriserver.global.websocket;

import com.example.urikkiriserver.global.utils.WebSocketUtils;
import com.example.urikkiriserver.global.websocket.dto.WebSocketMessage;
import com.example.urikkiriserver.global.websocket.dto.WebSocketMessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketKeepAliveScheduler {

    private final WebSocketSessionManager sessionManager;

    @Scheduled(fixedRate = 10000) // 10초마다 실행
    public void sendKeepAlive() {
        log.info("Sending KEEPALIVE to all WebSocket clients");
        // 로비 세션들에게 전송
        Set<WebSocketSession> lobbySessions = sessionManager.getAllLobbySessions();
        sendToSessions(lobbySessions, "Lobby");

        // 게임 세션들에게 전송
        Set<WebSocketSession> gameSessions = sessionManager.getAllGameSessions();
        sendToSessions(gameSessions, "Game");
    }

    private void sendToSessions(Set<WebSocketSession> sessions, String type) {
        if (sessions.isEmpty()) return;

        log.debug("Sending KEEPALIVE to {} sessions (Type: {})", sessions.size(), type);
        
        sessions.forEach(session -> {
            if (session.isOpen()) {
                try {
                    WebSocketUtils.sendObject(
                            WebSocketMessage.of(WebSocketMessageType.KEEPALIVE, "KEEPALIVE"),
                            session
                    );
                } catch (Exception e) {
                    log.error("Failed to send KEEPALIVE to {} session: {}", type, e.getMessage());
                }
            }
        });
    }
}
