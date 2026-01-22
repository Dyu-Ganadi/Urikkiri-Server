package com.example.urikkiriserver.global.websocket.dto;

import lombok.Builder;

@Builder
public record ExaminerSelectionDto(
        Long userId,
        SubmittedCardInfo selectedCard,
        String winnerNickname,
        int newBananaScore
) {
    public static ExaminerSelectionDto of(Long userId, SubmittedCardInfo selectedCard, String winnerNickname, int newBananaScore) {
        return ExaminerSelectionDto.builder()
                .userId(userId)
                .selectedCard(selectedCard)
                .winnerNickname(winnerNickname)
                .newBananaScore(newBananaScore)
                .build();
    }
}