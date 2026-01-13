package com.example.urikkiriserver.global.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

// WebSocket 세션을 방별로 관리하는 매니저
@Component
public class WebSocketSessionManager {

    // roomCode -> Set<WebSocketSession>
    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    // 방에 세션을 추가
    public void addSession(String roomCode, WebSocketSession session) {
        roomSessions.computeIfAbsent(roomCode, k -> new CopyOnWriteArraySet<>()).add(session);
    }

    // 방에서 세션을 제거
    public void removeSession(String roomCode, WebSocketSession session) {
        Set<WebSocketSession> sessions = roomSessions.get(roomCode);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomCode);
            }
        }
    }

    // 특정 방의 모든 세션 가져오기
    public Set<WebSocketSession> getSessionsByRoom(String roomCode) {
        return roomSessions.getOrDefault(roomCode, Set.of());
    }

    // 특정 세션이 속한 방 코드 찾기
    public String getRoomCodeBySession(WebSocketSession session) {
        return roomSessions.entrySet().stream()
                .filter(entry -> entry.getValue().contains(session))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}