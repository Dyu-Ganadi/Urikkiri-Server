package com.example.urikkiriserver.domain.card.presentation.dto.response;

import java.util.List;

public record CardListResponse(
        List<CardResponse> cards
) {
    public static CardListResponse of(List<CardResponse> cards) {
        return new CardListResponse(cards);
    }
}

