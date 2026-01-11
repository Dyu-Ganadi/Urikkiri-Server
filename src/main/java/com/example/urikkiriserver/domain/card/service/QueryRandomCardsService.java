package com.example.urikkiriserver.domain.card.service;

import com.example.urikkiriserver.domain.card.domain.Card;
import com.example.urikkiriserver.domain.card.domain.repository.CardRepository;
import com.example.urikkiriserver.domain.card.exception.InsufficientCardsException;
import com.example.urikkiriserver.domain.card.presentation.dto.response.CardListResponse;
import com.example.urikkiriserver.domain.card.presentation.dto.response.CardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueryRandomCardsService {

    private final CardRepository cardRepository;

    @Transactional(readOnly = true)
    public CardListResponse execute() {
        List<Card> cards = cardRepository.findRandomCards(5);

        if (cards.size() < 5) {
            throw InsufficientCardsException.EXCEPTION;
        }

        List<CardResponse> cardResponses = cards.stream()
                .map(CardResponse::from)
                .toList();

        return CardListResponse.of(cardResponses);
    }
}

