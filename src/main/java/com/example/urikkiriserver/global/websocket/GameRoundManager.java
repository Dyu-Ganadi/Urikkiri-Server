package com.example.urikkiriserver.global.websocket;

import com.example.urikkiriserver.global.websocket.dto.SubmittedCardInfo;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// 각 방의 게임 상태를 메모리에서 관리하는 매니저
@Component
public class GameRoundManager {

    // roomCode -> currentRound
    private final Map<String, Integer> roomRounds = new ConcurrentHashMap<>();

    // roomCode -> List<SubmittedCardInfo> (현재 라운드에 제출된 카드들)
    private final Map<String, List<SubmittedCardInfo>> submittedCards = new ConcurrentHashMap<>();

    public int getCurrentRound(String roomCode) {
        return roomRounds.getOrDefault(roomCode, 1);
    }

    public void startGame(String roomCode) {
        roomRounds.put(roomCode, 1);
        submittedCards.put(roomCode, Collections.synchronizedList(new ArrayList<>()));
    }

    public void nextRound(String roomCode) {
        roomRounds.put(roomCode, getCurrentRound(roomCode) + 1);
        // 새 라운드 시작 시 제출된 카드 초기화
        submittedCards.put(roomCode, Collections.synchronizedList(new ArrayList<>()));
    }

    public void endGame(String roomCode) {
        roomRounds.remove(roomCode);
        submittedCards.remove(roomCode);
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
}

