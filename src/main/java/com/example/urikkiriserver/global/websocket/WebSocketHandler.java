package com.example.urikkiriserver.global.websocket;

import com.example.urikkiriserver.domain.card.domain.repository.CardRepository;
import com.example.urikkiriserver.domain.card.exception.CardNotFoundException;
import com.example.urikkiriserver.domain.play.domain.Participant;
import com.example.urikkiriserver.domain.play.domain.repository.ParticipantRepository;
import com.example.urikkiriserver.domain.play.domain.repository.RoomRepository;
import com.example.urikkiriserver.domain.play.exception.RoomNotFoundException;
import com.example.urikkiriserver.domain.play.service.CreateRoomService;
import com.example.urikkiriserver.domain.play.service.JoinRoomService;
import com.example.urikkiriserver.domain.quiz.service.QueryRandomQuizService;
import com.example.urikkiriserver.domain.user.domain.User;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;
import com.example.urikkiriserver.global.websocket.dto.GameStartData;
import com.example.urikkiriserver.global.websocket.dto.SubmittedCardInfo;
import com.example.urikkiriserver.global.websocket.dto.WebSocketMessage;
import com.example.urikkiriserver.global.websocket.dto.WebSocketMessageType;
import com.example.urikkiriserver.global.websocket.exception.*;
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
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final CreateRoomService createRoomService;
    private final JoinRoomService joinRoomService;
    private final ObjectMapper objectMapper;
    private final QueryRandomQuizService queryRandomQuizService;
    private final GameRoundManager gameRoundManager;
    private final CardRepository cardRepository;
    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;

    @Override
    public void afterConnectionEstablished(@Nullable WebSocketSession session) {
        if (session == null) return;

        User user = (User) session.getAttributes().get("userPrincipal");
        if (user == null) {
            log.warn("User principal not found in session. Closing connection.");
            closeSession(session, WebSocketAuthenticationRequired.EXCEPTION);
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
                case SUBMIT_CARD -> handleSubmitCard(session, user, wsMessage);
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
            sendExceptionMessage(session, WebSocketInvalidMessageFormat.EXCEPTION);
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
            sendExceptionMessage(session, WebSocketRoomCreationFailed.EXCEPTION);
        }
    }

    private void handleJoinRoom(WebSocketSession session, User user, String roomCode) {
        if (roomCode == null || roomCode.isEmpty()) {
            sendExceptionMessage(session, WebSocketRoomCodeRequired.EXCEPTION);
            return;
        }

        try {
            // JoinRoomService를 통해 방 참가 로직 실행 및 참가자 목록 반환
            var joinRoomResponse = joinRoomService.execute(roomCode, user);

            // 세션을 방에 추가
            sessionManager.addSession(roomCode, session);

            // 1. 새로 입장한 사용자에게 전체 참가자 목록 전송
            sendMessage(session, WebSocketMessage.withData(
                WebSocketMessageType.ROOM_JOINED,
                roomCode,
                joinRoomResponse.participants(),  // 전체 참가자 목록
                "Successfully joined room"
            ));

            // 2. 기존 참가자들에게 새 참가자 정보만 브로드캐스트
            var newParticipant = joinRoomResponse.participants().stream()
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

            log.info("User {} joined room {} (total: {})",
                user.getNickname(), roomCode, joinRoomResponse.participants().size());

            // 3. 4명이 모이면 자동으로 게임 시작
            if (joinRoomResponse.participants().size() == 4) {
                log.info("Room {} is now full with 4 participants. Starting game automatically...", roomCode);

                // 게임 라운드 초기화
                gameRoundManager.startGame(roomCode);

                // 랜덤 질문 조회 (모든 참가자가 동일한 질문을 받음)
                var quiz = queryRandomQuizService.execute();

                // 참가자 정보 + 질문을 포함한 게임 시작 데이터
                var gameStartData = GameStartData.of(
                    joinRoomResponse.participants(),
                    quiz
                );

                var gameStartMessage = WebSocketMessage.withData(
                    WebSocketMessageType.GAME_START,
                    roomCode,
                    gameStartData,
                    "Game is starting! All 4 players are ready."
                );

                // 방의 모든 참가자에게 게임 시작 메시지 브로드캐스트
                sessionManager.getSessionsByRoom(roomCode)
                    .forEach(s -> sendMessage(s, gameStartMessage));
            }

        } catch (UrikkiriException e) {
            sendExceptionMessage(session, e);
        } catch (Exception e) {
            log.error("Unexpected error while joining room", e);
            sendExceptionMessage(session, WebSocketRoomNotFound.EXCEPTION);
        }
    }

    private void handleSubmitCard(WebSocketSession session, User user, WebSocketMessage wsMessage) {
        String roomCode = wsMessage.roomCode();
        if (roomCode == null || roomCode.isEmpty()) {
            sendExceptionMessage(session, WebSocketRoomCodeRequired.EXCEPTION);
            return;
        }

        try {
            // cardId 추출 (data 필드에서)
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.convertValue(wsMessage.data(), Map.class);
            Long cardId = ((Number) data.get("cardId")).longValue();

            // 카드 정보 조회
            var card = cardRepository.findById(cardId)
                .orElseThrow(() -> CardNotFoundException.EXCEPTION);

            // 참가자 정보 조회
            var room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> RoomNotFoundException.EXCEPTION);

            var participant = participantRepository.findByRoomIdIdAndUserIdId(room.getId(), user.getId())
                .orElseThrow(() -> new RuntimeException("Participant not found"));

            // 제출된 카드 정보 생성
            var submittedCardInfo = SubmittedCardInfo.of(participant, card);

            // 메모리에 저장
            gameRoundManager.submitCard(roomCode, submittedCardInfo);

            log.info("User {} submitted card {} in room {} (total submitted: {})",
                user.getNickname(), card.getWord(), roomCode, gameRoundManager.getSubmittedCount(roomCode));

            // 1. 제출한 사용자에게 확인 메시지
            sendMessage(session, WebSocketMessage.of(
                WebSocketMessageType.CARD_SUBMITTED,
                roomCode,
                "Card submitted successfully"
            ));

            // 2. 3명이 모두 제출했는지 확인
            if (gameRoundManager.isAllCardsSubmitted(roomCode)) {
                log.info("All cards submitted in room {}. Notifying examiner...", roomCode);

                // 제출된 모든 카드 조회
                var allSubmittedCards = gameRoundManager.getSubmittedCards(roomCode);

                // 출제자 찾기
                var participants = participantRepository.findAllByRoomIdIdWithUser(room.getId());
                var examiner = participants.stream()
                    .filter(Participant::isExaminer)
                    .findFirst()
                    .orElseThrow();

                // 출제자의 세션 찾기
                var examinerSession = sessionManager.getSessionsByRoom(roomCode).stream()
                    .filter(s -> {
                        User sessionUser = (User) s.getAttributes().get("userPrincipal");
                        return sessionUser != null && sessionUser.getId().equals(examiner.getUserId().getId());
                    })
                    .findFirst();

                // 출제자에게만 제출된 카드 목록 전송
                examinerSession.ifPresent(s -> sendMessage(s, WebSocketMessage.withData(
                    WebSocketMessageType.ALL_CARDS_SUBMITTED,
                    roomCode,
                    allSubmittedCards,
                    "All cards have been submitted"
                )));
            }

        } catch (UrikkiriException e) {
            sendExceptionMessage(session, e);
        } catch (Exception e) {
            log.error("Error handling card submission", e);
            sendExceptionMessage(session, WebSocketInvalidMessageFormat.EXCEPTION);
        }
    }

    private void closeSession(WebSocketSession session, UrikkiriException exception) {
        try {
            sendExceptionMessage(session, exception);
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException e) {
            log.error("Error closing WebSocket session", e);
        }
    }

    private void sendExceptionMessage(WebSocketSession session, UrikkiriException exception) {
        sendMessage(session, WebSocketMessage.of(
            WebSocketMessageType.ERROR,
            exception.getErrorCode().getMessage()
        ));
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
