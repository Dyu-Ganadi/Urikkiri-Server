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

    // roomCode -> Set<WebSocketSession> (로비 클라이언트용)
    private final Map<String, Set<WebSocketSession>> lobbyRoomSessions = new ConcurrentHashMap<>();

    // roomCode -> Set<WebSocketSession> (게임 클라이언트용)
    private final Map<String, Set<WebSocketSession>> gameRoomSessions = new ConcurrentHashMap<>();

    // 방에 로비 세션을 추가
    public void addLobbySession(String roomCode, WebSocketSession session) {
        lobbyRoomSessions.computeIfAbsent(roomCode, k -> new CopyOnWriteArraySet<>()).add(session);
    }

    // 방에 게임 세션을 추가
    public void addGameSession(String roomCode, WebSocketSession session) {
        gameRoomSessions.computeIfAbsent(roomCode, k -> new CopyOnWriteArraySet<>()).add(session);
    }

    // 방에 세션을 추가 (레거시 호환)
    public void addSession(String roomCode, WebSocketSession session) {
        addLobbySession(roomCode, session);
    }

    // 방에서 세션을 제거 (로비 및 게임 모두 확인)
    public void removeSession(String roomCode, WebSocketSession session) {
        // 로비 세션 제거
        Set<WebSocketSession> lobbySessions = lobbyRoomSessions.get(roomCode);
        if (lobbySessions != null) {
            lobbySessions.remove(session);
            if (lobbySessions.isEmpty()) {
                lobbyRoomSessions.remove(roomCode);
            }
        }

        // 게임 세션 제거
        Set<WebSocketSession> gameSessions = gameRoomSessions.get(roomCode);
        if (gameSessions != null) {
            gameSessions.remove(session);
            if (gameSessions.isEmpty()) {
                gameRoomSessions.remove(roomCode);
            }
        }
    }

    // 특정 방의 모든 로비 세션 가져오기
    public Set<WebSocketSession> getLobbySessionsByRoom(String roomCode) {
        return lobbyRoomSessions.getOrDefault(roomCode, Set.of());
    }

    // 특정 방의 모든 게임 세션 가져오기
    public Set<WebSocketSession> getGameSessionsByRoom(String roomCode) {
        return gameRoomSessions.getOrDefault(roomCode, Set.of());
    }

    // 특정 방의 모든 세션 가져오기 (레거시 호환)
    public Set<WebSocketSession> getSessionsByRoom(String roomCode) {
        return getLobbySessionsByRoom(roomCode);
    }

    // 특정 세션이 속한 방 코드 찾기
    public String getRoomCodeBySession(WebSocketSession session) {
        // 로비 세션 확인
        String roomCode = lobbyRoomSessions.entrySet().stream()
                .filter(entry -> entry.getValue().contains(session))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (roomCode != null) return roomCode;

        // 게임 세션 확인
        return gameRoomSessions.entrySet().stream()
                .filter(entry -> entry.getValue().contains(session))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}