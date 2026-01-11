package com.example.urikkiriserver.domain.play.presentation;

import com.example.urikkiriserver.domain.card.presentation.dto.response.CardListResponse;
import com.example.urikkiriserver.domain.card.service.QueryRandomCardsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/play-together")
public class PlayController {

    private final QueryRandomCardsService queryRandomCardsService;


    @GetMapping("/cards")
    public CardListResponse getCards() {
        return queryRandomCardsService.execute();
    }
}

