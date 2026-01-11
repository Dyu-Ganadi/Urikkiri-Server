package com.example.urikkiriserver.global.websocket.dto;

import com.example.urikkiriserver.domain.quiz.presentation.dto.response.QuizResponse;

import java.util.List;

public record GameStartData(
        List<ParticipantInfo> participants,
        QuizResponse question
) {
    public static GameStartData of(List<ParticipantInfo> participants, QuizResponse question) {
        return new GameStartData(participants, question);
    }
}

