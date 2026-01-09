package com.example.urikkiriserver.domain.play.presentation;

public record RoomResponse(
    String roomCode
) {
    public static RoomResponse of(String roomCode) {
        return new RoomResponse(roomCode);
    }
}
