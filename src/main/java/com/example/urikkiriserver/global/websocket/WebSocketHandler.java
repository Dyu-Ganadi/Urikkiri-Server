package com.example.urikkiriserver.global.websocket;

import com.example.urikkiriserver.domain.play.domain.repository.ParticipantRepository;
import com.example.urikkiriserver.domain.play.domain.repository.RoomRepository;
import com.example.urikkiriserver.global.websocket.dto.ParticipantInfo;
import com.example.urikkiriserver.global.websocket.dto.WebSocketMessage;
import com.example.urikkiriserver.global.websocket.dto.WebSocketMessageType;
import com.example.urikkiriserver.domain.play.service.CreateRoomService;
import com.example.urikkiriserver.domain.user.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final CreateRoomService createRoomService;
    private final CreateRoomService createRoomService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(@Nullable WebSocketSession session) {
        if (session == null) return;

        User user = (User) session.getAttributes().get("userPrincipal");
        if (user == null) {
            log.warn("User principal not found in session. Closing connection.");
            closeSession(session, "Authentication required");
            return;
        }

        // WebSocket 연결 성공 - roomCode는 나중에 메시지로 받음
        log.info("User {} connected to WebSocket (waiting for room action)", user.getNickname());

        // 연결 성공 메시지 전송
        sendMessage(session, WebSocketMessage.of(
            WebSocketMessageType.CONNECTED,
            "WebSocket connection established. Send CREATE_ROOM or JOIN_ROOM message."
        ));
    }

    @Override
    public void afterConnectionClosed(@Nullable WebSocketSession session, @NonNull CloseStatus status) {
        if (session == null) return;

        // 세션이 속한 방 코드 찾기
        String roomCode = sessionManager.getRoomCodeBySession(session);

        if (roomCode != null) {
            // 방에서 세션 제거
            sessionManager.removeSession(roomCode, session);

            User user = (User) session.getAttributes().get("userPrincipal");
            log.info("User {} disconnected from room {}", user != null ? user.getNickname() : "Unknown", roomCode);
        }
    }

    @Override
    protected void handleTextMessage(@Nullable WebSocketSession session, @NonNull TextMessage message) {
        if (session == null) return;

        User user = (User) session.getAttributes().get("userPrincipal");
        if (user == null) return;

        try {
            WebSocketMessage wsMessage = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);

            switch (wsMessage.type()) {
                case CREATE_ROOM -> handleCreateRoom(session, user);
                case JOIN_ROOM -> handleJoinRoom(session, user, wsMessage.roomCode());
                default -> {
                    String roomCode = sessionManager.getRoomCodeBySession(session);
                    if (roomCode != null) {
                        log.info("Message received in room {}: {}", roomCode, message.getPayload());
                        // 방의 다른 참가자들에게 메시지 브로드캐스트
                        // WebSocketUtils.sendToEachSocket(message, sessionManager.getSessionsByRoom(roomCode).toArray(new WebSocketSession[0]));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            sendMessage(session, WebSocketMessage.of(
                WebSocketMessageType.ERROR,
                "Invalid message format"
            ));
        }
    }

    private void handleCreateRoom(WebSocketSession session, User user) {
        try {
            var roomResponse = createRoomService.execute(user);
            String roomCode = roomResponse.roomCode();

            // 세션을 방에 추가
            sessionManager.addSession(roomCode, session);

            log.info("User {} created and joined room {}", user.getNickname(), roomCode);

            // 성공 메시지 전송
            sendMessage(session, WebSocketMessage.of(
                WebSocketMessageType.ROOM_CREATED,
                roomCode,
                "Room created successfully"
            ));

        } catch (Exception e) {
            log.error("Error creating room for user {}", user.getNickname(), e);
            sendMessage(session, WebSocketMessage.of(
                WebSocketMessageType.ERROR,
                "Failed to create room: " + e.getMessage()
            ));
        }
    }

    private void handleJoinRoom(WebSocketSession session, User user, String roomCode) {
        if (roomCode == null || roomCode.isEmpty()) {
            sendMessage(session, WebSocketMessage.of(
                WebSocketMessageType.ERROR,
                "Room code is required"
            ));
            return;
        }

        // 방이 존재하는지 확인
        var roomOptional = roomRepository.findByCode(roomCode);
        if (roomOptional.isEmpty()) {
            sendMessage(session, WebSocketMessage.of(
                WebSocketMessageType.ERROR,
                "Room does not exist"
            ));
            return;
        }

        var room = roomOptional.get();

        // 해당 사용자가 Participant로 등록되어 있는지 확인
        boolean isParticipant = participantRepository.existsByRoomIdIdAndUserIdId(
            room.getId(),
            user.getId()
        );

        if (!isParticipant) {
            sendMessage(session, WebSocketMessage.of(
                WebSocketMessageType.ERROR,
                "You are not a participant of this room"
            ));
            return;
        }

        // 세션을 방에 추가
        sessionManager.addSession(roomCode, session);

        // 전체 참가자 목록 조회
        var participants = participantRepository.findAllByRoomIdId(room.getId())
            .stream()
            .map(ParticipantInfo::from)
            .toList();

        // 1. 새로 입장한 사용자에게 전체 참가자 목록 전송
        sendMessage(session, WebSocketMessage.withData(
            WebSocketMessageType.ROOM_JOINED,
            roomCode,
            participants,  // 전체 참가자 목록
            "Successfully joined room"
        ));

        // 2. 기존 참가자들에게 새 참가자 정보만 브로드캐스트
        var newParticipant = participants.stream()
            .filter(p -> p.userId().equals(user.getId()))
            .findFirst()
            .orElseThrow();

        var broadcastMessage = WebSocketMessage.withData(
            WebSocketMessageType.USER_JOINED,
            roomCode,
            newParticipant,  // 새 참가자 한 명만
            user.getNickname() + " joined the room"
        );

        // 자기 자신을 제외한 기존 참가자들에게만 전송
        sessionManager.getSessionsByRoom(roomCode).stream()
            .filter(s -> !s.getId().equals(session.getId()))
            .forEach(s -> sendMessage(s, broadcastMessage));

        log.info("User {} joined room {} (total participants: {})",
            user.getNickname(), roomCode, participants.size());
    }

    private void closeSession(WebSocketSession session, String reason) {
        try {
            sendMessage(session, WebSocketMessage.of(
                WebSocketMessageType.ERROR,
                reason
            ));
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException e) {
            log.error("Error closing WebSocket session", e);
        }
    }

    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Error sending WebSocket message", e);
        }
    }
}
