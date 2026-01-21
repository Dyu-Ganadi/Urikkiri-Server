package com.example.urikkiriserver.global.websocket.dto;

import com.example.urikkiriserver.domain.card.domain.Card;
import com.example.urikkiriserver.domain.play.domain.Participant;

public record SubmittedCardInfo(
        Long participantId,
        String nickname,
        Long cardId,
        String word,
        String meaning
) {
    public static SubmittedCardInfo of(Participant participant, Card card) {
        return new SubmittedCardInfo(
                participant.getId(),
                participant.getUserId().getNickname(),
                card.getId(),
                card.getWord(),
                card.getMeaning()
        );
    }
}
