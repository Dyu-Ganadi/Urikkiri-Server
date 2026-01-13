package com.example.urikkiriserver.global.websocket;

import com.example.urikkiriserver.global.websocket.dto.SubmittedCardInfo;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// 각 방의 게임 상태를 메모리에서 관리하는 매니저
@Component
public class GameRoundManager {

    // roomCode -> List<SubmittedCardInfo> (현재 제출된 카드들)
    private final Map<String, List<SubmittedCardInfo>> submittedCards = new ConcurrentHashMap<>();

    // roomCode -> List<Long> (출제자였던 participantId 히스토리)
    private final Map<String, List<Long>> examinerHistory = new ConcurrentHashMap<>();

    // roomCode -> 게임 시작 여부
    private final Map<String, Boolean> gameStarted = new ConcurrentHashMap<>();

    public void startGame(String roomCode) {
        gameStarted.put(roomCode, true);
        submittedCards.put(roomCode, Collections.synchronizedList(new ArrayList<>()));
        examinerHistory.put(roomCode, Collections.synchronizedList(new ArrayList<>()));
    }

    public void clearSubmittedCards(String roomCode) {
        // 다음 턴 시작 시 제출된 카드 초기화
        submittedCards.put(roomCode, Collections.synchronizedList(new ArrayList<>()));
    }

    public void endGame(String roomCode) {
        gameStarted.remove(roomCode);
        submittedCards.remove(roomCode);
        examinerHistory.remove(roomCode);
    }

    // 카드 제출
    public void submitCard(String roomCode, SubmittedCardInfo cardInfo) {
        submittedCards.computeIfAbsent(roomCode, k -> new ArrayList<>()).add(cardInfo);
    }

    // 제출된 카드 목록 조회
    public List<SubmittedCardInfo> getSubmittedCards(String roomCode) {
        return new ArrayList<>(submittedCards.getOrDefault(roomCode, Collections.emptyList()));
    }

    // 제출된 카드 개수 확인
    public int getSubmittedCount(String roomCode) {
        return submittedCards.getOrDefault(roomCode, Collections.emptyList()).size();
    }

    // 3명이 모두 제출했는지 확인 (출제자 제외)
    public boolean isAllCardsSubmitted(String roomCode) {
        return getSubmittedCount(roomCode) >= 3;
    }

    public boolean isGameStarted(String roomCode) {
        return gameStarted.getOrDefault(roomCode, false);
    }

    // 출제자 히스토리에 추가
    public void addExaminerHistory(String roomCode, Long participantId) {
        examinerHistory.computeIfAbsent(roomCode, k -> Collections.synchronizedList(new ArrayList<>())).add(participantId);
    }

    // 출제자 히스토리 조회
    public List<Long> getExaminerHistory(String roomCode) {
        return new ArrayList<>(examinerHistory.getOrDefault(roomCode, Collections.emptyList()));
    }

    // 다음 출제자 Participant ID 선택 (출제자가 아니었던 사람 중 랜덤)
    public Long selectNextExaminer(String roomCode, List<Long> allParticipantIds) {
        List<Long> history = getExaminerHistory(roomCode);

        // 아직 출제자가 아니었던 참가자 필터링
        List<Long> availableParticipants = allParticipantIds.stream()
                .filter(id -> !history.contains(id))
                .toList();

        // 모두 출제자를 했다면 히스토리 초기화하고 모든 참가자 중에서 선택
        if (availableParticipants.isEmpty()) {
            examinerHistory.put(roomCode, Collections.synchronizedList(new ArrayList<>()));
            availableParticipants = new ArrayList<>(allParticipantIds);
        }

        // 랜덤으로 선택
        Random random = new Random();
        return availableParticipants.get(random.nextInt(availableParticipants.size()));
    }
}

