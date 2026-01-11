package com.example.urikkiriserver.global.websocket.dto;

public enum WebSocketMessageType {
    CONNECTED,

    CREATE_ROOM,
    ROOM_CREATED,
    JOIN_ROOM,
    ROOM_JOINED,
    USER_JOINED,
    LEAVE_ROOM,

    GAME_START,

    ERROR
}

