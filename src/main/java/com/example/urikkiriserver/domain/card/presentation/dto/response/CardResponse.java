package com.example.urikkiriserver.domain.card.presentation.dto.response;

import com.example.urikkiriserver.domain.card.domain.Card;

public record CardResponse(
        Long cardId,
        String word,
        String meaning
) {
    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.getWord(),
                card.getMeaning()
        );
    }
}

