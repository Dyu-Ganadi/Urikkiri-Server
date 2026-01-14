package com.example.urikkiriserver.global.websocket.dto;

public enum WebSocketMessageType {
    CONNECTED,

    CREATE_ROOM,
    ROOM_CREATED,
    JOIN_ROOM,
    ROOM_JOINED,
    USER_JOINED,
    ROOM_EXIT,

    GAME_READY,           // 4명 모임 → 게임 서버 연결 안내
    CONNECT_GAME,         // 클라이언트 → 게임 서버 연결 요청
    GAME_START,           // 게임 시작 (4명 모두 연결 완료 시, 질문 포함)
    SUBMIT_CARD,
    CARD_SUBMITTED,
    ALL_CARDS_SUBMITTED,
    EXAMINER_SELECT,      // 출제자가 카드 선택
    EXAMINER_SELECTED,    // 출제자 선택 완료
    NEXT_ROUND,           // 다음 라운드 시작 (새 출제자 + 새 질문)
    ROUND_END,            // 게임 종료 (5점 달성)

    ERROR
}