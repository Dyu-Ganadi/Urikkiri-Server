package com.example.urikkiriserver.domain.play.presentation.dto.response;

import com.example.urikkiriserver.global.websocket.dto.ParticipantInfo;

import java.util.List;

public record RoomResponse(
    String roomCode,
    List<ParticipantInfo> participants
) {
    public static RoomResponse of(String roomCode, List<ParticipantInfo> participants) {
        return new RoomResponse(roomCode, participants);
    }
}
