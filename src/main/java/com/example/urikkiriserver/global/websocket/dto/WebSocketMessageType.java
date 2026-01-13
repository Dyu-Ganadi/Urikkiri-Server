package com.example.urikkiriserver.global.websocket.dto;

public enum WebSocketMessageType {
    CONNECTED,

    CREATE_ROOM,
    ROOM_CREATED,
    JOIN_ROOM,
    ROOM_JOINED,
    USER_JOINED,
    ROOM_EXIT,

    GAME_START,
    SUBMIT_CARD,
    CARD_SUBMITTED,
    ALL_CARDS_SUBMITTED,
    EXAMINER_SELECT,      // 출제자가 카드 선택
    EXAMINER_SELECTED,    // 출제자 선택 완료 (다음 라운드 시작)
    ROUND_END,            // 게임 종료 (5점 달성)

    ERROR
}