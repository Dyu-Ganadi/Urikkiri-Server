package com.example.urikkiriserver.domain.card.service;

import com.example.urikkiriserver.domain.card.domain.repository.CardRepository;
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
        List<CardResponse> cards = cardRepository.findRandomCards(5)
                .stream()
                .map(CardResponse::from)
                .toList();

        return CardListResponse.of(cards);
    }
}

