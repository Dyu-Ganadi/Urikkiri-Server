package com.example.urikkiriserver.global.websocket.dto;

import lombok.Builder;

@Builder
public record ExaminerSelectionDto(
        Long userId,
        String cardWord,
        String winnerNickname,
        int newBananaScore
) {
    public static ExaminerSelectionDto of(Long userId, String cardWord, String winnerNickname, int newBananaScore) {
        return ExaminerSelectionDto.builder()
                .userId(userId)
                .cardWord(cardWord)
                .winnerNickname(winnerNickname)
                .newBananaScore(newBananaScore)
                .build();
    }
}