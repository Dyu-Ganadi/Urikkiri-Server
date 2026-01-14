package com.example.urikkiriserver.global.websocket;

import com.example.urikkiriserver.domain.card.domain.repository.CardRepository;
import com.example.urikkiriserver.domain.card.exception.CardNotFoundException;
import com.example.urikkiriserver.domain.play.domain.Participant;
import com.example.urikkiriserver.domain.play.domain.repository.ParticipantRepository;
import com.example.urikkiriserver.domain.play.domain.repository.RoomRepository;
import com.example.urikkiriserver.domain.play.exception.RoomNotFoundException;
import com.example.urikkiriserver.domain.play.exception.ParticipantNotFoundException;
import com.example.urikkiriserver.domain.play.exception.ExaminerNotFoundException;
import com.example.urikkiriserver.domain.play.exception.ExaminerCannotSubmitCardException;
import com.example.urikkiriserver.domain.play.service.CreateRoomService;
import com.example.urikkiriserver.domain.play.service.JoinRoomService;
import com.example.urikkiriserver.domain.quiz.service.QueryRandomQuizService;
import com.example.urikkiriserver.domain.user.domain.User;
import com.example.urikkiriserver.domain.user.domain.repository.UserRepository;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;
import com.example.urikkiriserver.global.websocket.dto.*;
import com.example.urikkiriserver.global.websocket.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;

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
    private final UserRepository userRepository;

    @Override
    public void afterConnectionEstablished(@Nullable WebSocketSession session) {
        if (session == null) return;

        User user = (User) session.getAttributes().get("userPrincipal");
        ClientType clientType = (ClientType) session.getAttributes().get("clientType");

        if (user == null) {
            log.warn("User principal not found in session. Closing connection.");
            sendExceptionMessage(session, WebSocketAuthenticationRequired.EXCEPTION);
            closeSession(session);
            return;
        }

        if (clientType == null) {
            clientType = ClientType.LOBBY; // 기본값
        }

        log.info("User {} connected to WebSocket as {} client", user.getNickname(), clientType);

        // 클라이언트 타입별 연결 메시지
        String connectionMessage = clientType == ClientType.LOBBY
            ? "WebSocket connection established. Send CREATE_ROOM or JOIN_ROOM message."
            : "Game WebSocket connection established. Send CONNECT_GAME message with room code.";

        sendMessage(session, WebSocketMessage.of(
                WebSocketMessageType.CONNECTED,
                connectionMessage
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
                case ROOM_EXIT -> handleRoomExit(session, user, wsMessage.roomCode());
                case CONNECT_GAME -> handleConnectGame(session, user, wsMessage.roomCode());
                case SUBMIT_CARD -> handleSubmitCard(session, user, wsMessage);
                case EXAMINER_SELECT -> handleExaminerSelect(session, user, wsMessage);
                default -> {
                    String roomCode = sessionManager.getRoomCodeBySession(session);
                    if (roomCode != null) {
                        log.info("Message received in room {}: {}", roomCode, message.getPayload());
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

            // 세션을 방에 추가 (로비 세션으로)
            sessionManager.addSession(roomCode, session);

            // 1. 새로 입장한 사용자에게 전체 참가자 목록 전송
            sendMessage(session, WebSocketMessage.withData(
                    WebSocketMessageType.ROOM_JOINED,
                    roomCode,
                    joinRoomResponse.participants(),
                    "Successfully joined room"
            ));

            // 2. 기존 참가자들에게 새 참가자 정보만 브로드캐스트
            var newParticipant = joinRoomResponse.participants().stream()
                    .filter(p -> p.userId().equals(user.getId()))
                    .findFirst()
                    .orElseThrow(() -> ParticipantNotFoundException.EXCEPTION);

            var broadcastMessage = WebSocketMessage.withData(
                    WebSocketMessageType.USER_JOINED,
                    roomCode,
                    newParticipant,
                    user.getNickname() + " joined the room"
            );

            // 자기 자신을 제외한 기존 참가자들에게만 전송
            sessionManager.getLobbySessionsByRoom(roomCode).stream()
                    .filter(s -> !s.getId().equals(session.getId()))
                    .forEach(s -> sendMessage(s, broadcastMessage));

            log.info("User {} joined room {} (total: {})",
                    user.getNickname(), roomCode, joinRoomResponse.participants().size());

            // 3. 4명이 모이면 게임 준비 완료 알림만 전송 (Unity가 재연결해야 함)
            if (joinRoomResponse.participants().size() == 4) {
                log.info("Room {} is now full. Notifying clients to launch Unity game...", roomCode);

                // 게임 준비 완료 데이터
                var gameReadyData = GameReadyData.of(joinRoomResponse.participants());

                var gameReadyMessage = WebSocketMessage.withData(
                        WebSocketMessageType.GAME_READY,
                        roomCode,
                        gameReadyData,
                        "All players ready! Launch Unity game with your token and room code."
                );

                // 방의 모든 로비 클라이언트에게 게임 준비 메시지 브로드캐스트
                sessionManager.getLobbySessionsByRoom(roomCode)
                        .forEach(s -> sendMessage(s, gameReadyMessage));
            }

        } catch (UrikkiriException e) {
            sendExceptionMessage(session, e);
        } catch (Exception e) {
            log.error("Unexpected error while joining room", e);
            sendExceptionMessage(session, WebSocketRoomNotFound.EXCEPTION);
        }
    }

    /**
     * 클라이언트가 게임 서버로 연결하는 핸들러
     * GAME_READY 받은 후 클라이언트가 다시 토큰+방코드로 연결
     */
    private void handleConnectGame(WebSocketSession session, User user, String roomCode) {
        if (roomCode == null || roomCode.isEmpty()) {
            sendExceptionMessage(session, WebSocketRoomCodeRequired.EXCEPTION);
            return;
        }

        try {
            // 방과 참가자 검증
            var room = roomRepository.findByCode(roomCode)
                    .orElseThrow(() -> RoomNotFoundException.EXCEPTION);

            var participant = participantRepository.findByRoomIdIdAndUserIdId(room.getId(), user.getId())
                    .orElseThrow(() -> ParticipantNotFoundException.EXCEPTION);

            // 게임 세션에 추가
            sessionManager.addGameSession(roomCode, session);

            log.info("User {} connected to game server for room {} ({}/4)",
                    user.getNickname(), roomCode, sessionManager.getGameSessionsByRoom(roomCode).size());

            // 게임 세션에 4명이 모두 연결되었는지 확인
            var gameSessions = sessionManager.getGameSessionsByRoom(roomCode);
            var participants = participantRepository.findAllByRoomIdIdWithUser(room.getId());

            if (gameSessions.size() == participants.size()) {
                log.info("All 4 players connected to game server for room {}. Starting game...", roomCode);

                // 모든 플레이어가 게임 서버에 연결되면 게임 시작
                startGameForConnectedPlayers(roomCode, participants);
            }

        } catch (UrikkiriException e) {
            sendExceptionMessage(session, e);
        } catch (Exception e) {
            log.error("Error connecting to game server", e);
            sendExceptionMessage(session, WebSocketRoomNotFound.EXCEPTION);
        }
    }

    /**
     * 모든 플레이어가 게임 서버에 연결된 후 실제 게임을 시작
     */
    private void startGameForConnectedPlayers(String roomCode, List<Participant> participants) {
        try {
            // 게임 라운드 초기화
            gameRoundManager.startGame(roomCode);

            // 현재 출제자 찾기 및 히스토리에 추가
            var currentExaminer = participants.stream()
                    .filter(Participant::isExaminer)
                    .findFirst()
                    .orElseThrow(() -> ExaminerNotFoundException.EXCEPTION);
            gameRoundManager.addExaminerHistory(roomCode, currentExaminer.getId());

            // 랜덤 질문 조회
            var quiz = queryRandomQuizService.execute();

            // 참가자 정보 리스트 생성
            var participantInfoList = participants.stream()
                    .map(p -> ParticipantInfo.of(
                            p.getUserId().getId(),
                            p.getUserId().getNickname(),
                            p.getUserId().getLevel(),
                            p.isExaminer()
                    ))
                    .toList();

            // 게임 시작 데이터
            var gameStartData = GameStartData.of(participantInfoList, quiz);

            var gameStartMessage = WebSocketMessage.withData(
                    WebSocketMessageType.GAME_START,
                    roomCode,
                    gameStartData,
                    "Game is starting! All 4 players connected."
            );

            // 게임 세션에 연결된 모든 플레이어에게 게임 시작 메시지 전송
            sessionManager.getGameSessionsByRoom(roomCode)
                    .forEach(s -> sendMessage(s, gameStartMessage));

            log.info("Game started for room {} with {} players", roomCode, participants.size());

        } catch (Exception e) {
            log.error("Error starting game for room {}", roomCode, e);
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
                    .orElseThrow(() -> ParticipantNotFoundException.EXCEPTION);

            // 출제자는 카드를 제출할 수 없음
            if (participant.isExaminer()) {
                throw ExaminerCannotSubmitCardException.EXCEPTION;
            }

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
                        .orElseThrow(() -> ExaminerNotFoundException.EXCEPTION);

                // 출제자의 게임 세션 찾기
                var examinerSession = sessionManager.getGameSessionsByRoom(roomCode).stream()
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

    @Transactional
    protected void handleExaminerSelect(WebSocketSession session, User user, WebSocketMessage wsMessage) {
        String roomCode = wsMessage.roomCode();
        if (roomCode == null || roomCode.isEmpty()) {
            sendExceptionMessage(session, WebSocketRoomCodeRequired.EXCEPTION);
            return;
        }

        try {
            // participantId 추출
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.convertValue(wsMessage.data(), Map.class);
            if (data == null || data.get("participantId") == null) {
                sendExceptionMessage(session, WebSocketInvalidMessageFormat.EXCEPTION);
                return;
            }
            Long selectedParticipantId = ((Number) data.get("participantId")).longValue();

            // Room 조회
            var room = roomRepository.findByCode(roomCode)
                    .orElseThrow(() -> RoomNotFoundException.EXCEPTION);

            // 현재 사용자가 출제자인지 확인
            var examiner = participantRepository.findByRoomIdIdAndUserIdId(room.getId(), user.getId())
                    .orElseThrow(() -> ParticipantNotFoundException.EXCEPTION);

            if (!examiner.isExaminer()) {
                sendExceptionMessage(session, WebSocketInvalidMessageFormat.EXCEPTION);
                return;
            }

            // 선택된 참가자 조회
            var winner = participantRepository.findByRoomIdIdAndUserIdId(room.getId(), selectedParticipantId)
                    .orElseThrow(() -> ParticipantNotFoundException.EXCEPTION);

            // 승자의 bananaScore 증가
            winner.winGame();
            participantRepository.save(winner);

            // 제출된 카드에서 승자의 카드 정보 찾기
            var submittedCards = gameRoundManager.getSubmittedCards(roomCode);
            var winnerCard = submittedCards.stream()
                    .filter(card -> card.participantId().equals(selectedParticipantId))
                    .findFirst()
                    .orElseThrow(() -> CardNotFoundException.EXCEPTION);

            log.info("Examiner {} selected participant {} (score: {}) in room {}",
                    user.getNickname(), winner.getUserId().getNickname(), winner.getBananaScore(), roomCode);

            // 모든 참가자에게 출제자의 선택 알림
            var selectionDto = ExaminerSelectionDto.of(
                    selectedParticipantId,
                    winnerCard.word(),
                    winner.getUserId().getNickname(),
                    winner.getBananaScore()
            );

            var selectionMessage = WebSocketMessage.withData(
                    WebSocketMessageType.EXAMINER_SELECTED,
                    roomCode,
                    selectionDto,
                    "Examiner has selected a card"
            );

            sessionManager.getGameSessionsByRoom(roomCode)
                    .forEach(s -> sendMessage(s, selectionMessage));

            // 5점 달성 여부 확인
            if (winner.getBananaScore() >= 5) {
                log.info("Game ended in room {}. Winner: {} with 5 points", roomCode, winner.getUserId().getNickname());
                endGame(roomCode, room.getId());
            } else {
                // 다음 턴으로 진행 - 제출된 카드 초기화
                gameRoundManager.clearSubmittedCards(roomCode);

                // 출제자 교체 로직
                var allParticipants = participantRepository.findAllByRoomIdIdWithUser(room.getId());
                var participantIds = allParticipants.stream()
                        .map(Participant::getId)
                        .toList();

                // 다음 출제자 선택 (Participant ID 기준)
                Long nextExaminerParticipantId = gameRoundManager.selectNextExaminer(roomCode, participantIds);
                gameRoundManager.addExaminerHistory(roomCode, nextExaminerParticipantId);

                // 현재 출제자를 false로 설정
                examiner.setExaminer(false);

                // 새 출제자를 true로 설정
                var nextExaminer = allParticipants.stream()
                        .filter(p -> p.getId().equals(nextExaminerParticipantId))
                        .findFirst()
                        .orElseThrow(() -> ParticipantNotFoundException.EXCEPTION);
                nextExaminer.setExaminer(true);

                // DB에 저장
                participantRepository.save(examiner);
                participantRepository.save(nextExaminer);

                // 새로운 질문 조회
                var nextQuiz = queryRandomQuizService.execute();

                log.info("Next turn in room {}. New examiner: {}", roomCode, nextExaminer.getUserId().getNickname());

                // 다음 턴 정보 전송
                var nextRoundData = NextRoundData.of(
                        nextExaminer.getUserId().getId(),
                        nextExaminer.getUserId().getNickname(),
                        nextQuiz
                );

                var nextRoundMessage = WebSocketMessage.withData(
                        WebSocketMessageType.NEXT_ROUND,
                        roomCode,
                        nextRoundData,
                        "Next turn is starting!"
                );

                // 모든 게임 참가자에게 다음 턴 시작 알림
                sessionManager.getGameSessionsByRoom(roomCode)
                        .forEach(s -> sendMessage(s, nextRoundMessage));
            }

        } catch (UrikkiriException e) {
            sendExceptionMessage(session, e);
        } catch (Exception e) {
            log.error("Error handling examiner selection", e);
            sendExceptionMessage(session, WebSocketInvalidMessageFormat.EXCEPTION);
        }
    }

    @Transactional
    protected void endGame(String roomCode, Long roomId) {
        try {
            // 모든 참가자 조회
            var participants = participantRepository.findAllByRoomIdIdWithUser(roomId);

            // bananaScore 기준으로 내림차순 정렬
            var sortedParticipants = participants.stream()
                    .sorted(Comparator.comparingInt(Participant::getBananaScore).reversed())
                    .toList();

            // 순위별 경험치 배열 (1위: 20, 2위: 10, 3위: 5, 4위: 2)
            int[] xpRewards = {20, 10, 5, 2};

            // 순위 정보 생성 및 경험치 지급
            List<PlayerRankInfo> rankings = new ArrayList<>();
            for (int i = 0; i < sortedParticipants.size() && i < xpRewards.length; i++) {
                var participant = sortedParticipants.get(i);
                var user = participant.getUserId();

                // 경험치 추가 및 레벨 자동 계산
                int xp = xpRewards[i];
                user.addXp(xp);
                userRepository.save(user);

                // 순위 정보 생성
                rankings.add(PlayerRankInfo.of(
                        i + 1,
                        user,
                        participant.getBananaScore()
                ));

                log.info("Rank {}: {} (Score: {}, XP +{}, Total XP: {}, Level: {})",
                        i + 1, user.getNickname(), participant.getBananaScore(), xp, user.getBananaxp(), user.getLevel());
            }

            // 게임 결과 메시지 생성
            var gameResult = GameResultDto.of(rankings);
            var endMessage = WebSocketMessage.withData(
                    WebSocketMessageType.ROUND_END,
                    roomCode,
                    gameResult,
                    "Game has ended!"
            );

            // 모든 게임 참가자에게 게임 종료 메시지 전송
            sessionManager.getGameSessionsByRoom(roomCode)
                    .forEach(s -> sendMessage(s, endMessage));

            // 게임 상태 정리
            gameRoundManager.endGame(roomCode);

            log.info("Game ended in room {}. Final rankings sent to all participants.", roomCode);

        } catch (Exception e) {
            log.error("Error ending game in room {}", roomCode, e);

            // 모든 게임 참가자에게 에러 알림
            var errorMessage = WebSocketMessage.of(
                    WebSocketMessageType.ERROR,
                    "게임 종료 중 오류가 발생했습니다."
            );
            sessionManager.getGameSessionsByRoom(roomCode)
                    .forEach(s -> sendMessage(s, errorMessage));
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

    private void closeSession(WebSocketSession session) {
        try {
            session.close();
        } catch (IOException e) {
            log.error("Error closing WebSocket session", e);
        }
    }

    @Transactional
    protected void handleRoomExit(WebSocketSession session, User user, String roomCode) {
        if (roomCode == null || roomCode.isEmpty()) {
            sendExceptionMessage(session, WebSocketRoomCodeRequired.EXCEPTION);
            return;
        }

        try {
            // Room 조회
            var room = roomRepository.findByCode(roomCode)
                    .orElseThrow(() -> RoomNotFoundException.EXCEPTION);

            // 게임이 시작되었는지 확인
            if (gameRoundManager.isGameStarted(roomCode)) {
                sendMessage(session, WebSocketMessage.of(
                        WebSocketMessageType.ERROR,
                        "게임이 시작된 후에는 방을 나갈 수 없습니다."
                ));
                return;
            }

            // Participant 조회
            var participant = participantRepository.findByRoomIdIdAndUserIdId(room.getId(), user.getId())
                    .orElseThrow(() -> ParticipantNotFoundException.EXCEPTION);

            // Participant 삭제
            participantRepository.delete(participant);

            // 세션 제거
            sessionManager.removeSession(roomCode, session);

            log.info("User {} exited room {} before game start", user.getNickname(), roomCode);

            // 1. 나간 사용자에게 확인 메시지
            sendMessage(session, WebSocketMessage.of(
                    WebSocketMessageType.ROOM_EXIT,
                    roomCode,
                    "Successfully exited the room"
            ));

            // 2. 남은 참가자들에게 알림
            var remainingParticipants = participantRepository.findAllByRoomIdIdWithUser(room.getId());

            if (!remainingParticipants.isEmpty()) {
                var exitNotification = UserExitDto.of(
                        user.getId(),
                        user.getNickname(),
                        remainingParticipants.size()
                );

                var exitMessage = WebSocketMessage.withData(
                        WebSocketMessageType.ROOM_EXIT,
                        roomCode,
                        exitNotification,
                        user.getNickname() + " has left the room"
                );

                // 남은 로비 참가자들에게만 브로드캐스트
                sessionManager.getLobbySessionsByRoom(roomCode)
                        .forEach(s -> sendMessage(s, exitMessage));

                log.info("Room {} now has {} participants remaining", roomCode, remainingParticipants.size());
            } else {
                log.info("Room {} is now empty. Consider cleanup.", roomCode);
            }

        } catch (UrikkiriException e) {
            sendExceptionMessage(session, e);
        } catch (Exception e) {
            log.error("Error handling room exit", e);
            sendExceptionMessage(session, WebSocketInvalidMessageFormat.EXCEPTION);
        }
    }

}

