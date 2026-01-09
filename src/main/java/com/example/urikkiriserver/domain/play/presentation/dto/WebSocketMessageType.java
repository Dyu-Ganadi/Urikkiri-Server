package com.example.urikkiriserver.domain.play.presentation.dto;

public enum WebSocketMessageType {
    CONNECTED,

    CREATE_ROOM,
    ROOM_CREATED,
    JOIN_ROOM,
    ROOM_JOINED,
    LEAVE_ROOM,

    ERROR
}
