package com.example.urikkiriserver.domain.quiz.presentation.dto.response;

import com.example.urikkiriserver.domain.quiz.domain.Quiz;

public record QuizResponse(
        Long quizId,
        String content
) {
    public static QuizResponse from(Quiz quiz) {
        return new QuizResponse(
                quiz.getId(),
                quiz.getContent()
        );
    }
}

