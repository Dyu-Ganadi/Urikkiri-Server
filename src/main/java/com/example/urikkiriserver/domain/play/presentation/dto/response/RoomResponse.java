package com.example.urikkiriserver.domain.play.presentation.dto.response;

public record RoomResponse(
    String roomCode
) {
    public static RoomResponse of(String roomCode) {
        return new RoomResponse(roomCode);
    }
}
