package com.example.urikkiriserver.global.websocket.dto;

import lombok.Builder;

@Builder
public record ExaminerSelectionDto(
        Long participantId,
        String cardWord,
        String winnerNickname,
        int newBananaScore
) {
    public static ExaminerSelectionDto of(Long participantId, String cardWord, String winnerNickname, int newBananaScore) {
        return ExaminerSelectionDto.builder()
                .participantId(participantId)
                .cardWord(cardWord)
                .winnerNickname(winnerNickname)
                .newBananaScore(newBananaScore)
                .build();
    }
}