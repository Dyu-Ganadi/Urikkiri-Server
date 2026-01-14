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

    // User 정보로 ParticipantInfo 생성 (게임 시작용)
    public static ParticipantInfo of(Long userId, String nickname, int level, boolean isExaminer) {
        return new ParticipantInfo(userId, nickname, level, isExaminer);
    }
}
