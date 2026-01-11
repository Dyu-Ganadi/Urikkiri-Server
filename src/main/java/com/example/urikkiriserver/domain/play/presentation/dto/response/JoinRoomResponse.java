package com.example.urikkiriserver.domain.play.presentation.dto.response;

import com.example.urikkiriserver.domain.play.domain.Room;
import com.example.urikkiriserver.global.websocket.dto.ParticipantInfo;

import java.util.List;

public record JoinRoomResponse(
    String roomCode,
    List<ParticipantInfo> participants
) {
    public static JoinRoomResponse of(Room room, List<ParticipantInfo> participants) {
        return new JoinRoomResponse(room.getCode(), participants);
    }
}
