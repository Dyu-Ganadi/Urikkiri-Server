package com.example.urikkiriserver.domain.play.presentation.dto;

import com.example.urikkiriserver.domain.play.domain.Participant;

public record ParticipantInfo(
    Long userId,
    String nickname,
    int bananaScore
) {
    public static ParticipantInfo from(Participant participant) {
        return new ParticipantInfo(
            participant.getUserId().getId(),
            participant.getUserId().getNickname(),
            participant.getUserId().getLevel()
        );
    }
}
