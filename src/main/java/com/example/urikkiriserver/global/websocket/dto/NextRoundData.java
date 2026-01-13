package com.example.urikkiriserver.global.websocket.dto;

import com.example.urikkiriserver.domain.quiz.presentation.dto.response.QuizResponse;
import lombok.Builder;

@Builder
public record NextRoundData(
        Long newExaminerId,
        String newExaminerNickname,
        QuizResponse quiz
) {
    public static NextRoundData of(Long newExaminerId, String newExaminerNickname, QuizResponse quiz) {
        return NextRoundData.builder()
                .newExaminerId(newExaminerId)
                .newExaminerNickname(newExaminerNickname)
                .quiz(quiz)
                .build();
    }
}

