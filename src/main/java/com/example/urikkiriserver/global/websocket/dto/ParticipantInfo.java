package com.example.urikkiriserver.global.websocket.dto;

import com.example.urikkiriserver.domain.play.domain.Participant;

public record ParticipantInfo(
    Long userId,
    String nickname,
    int level,
    boolean isExaminer
) {
    public static ParticipantInfo from(Participant participant) {
        return new ParticipantInfo(
            participant.getUserId().getId(),
            participant.getUserId().getNickname(),
            participant.getUserId().getLevel(),
            participant.isExaminer()
        );
    }
}

