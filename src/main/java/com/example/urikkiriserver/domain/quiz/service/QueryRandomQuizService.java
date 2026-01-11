package com.example.urikkiriserver.domain.quiz.service;

import com.example.urikkiriserver.domain.quiz.domain.Quiz;
import com.example.urikkiriserver.domain.quiz.domain.repository.QuizRepository;
import com.example.urikkiriserver.domain.quiz.exception.QuizNotFoundException;
import com.example.urikkiriserver.domain.quiz.presentation.dto.response.QuizResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QueryRandomQuizService {

    private final QuizRepository quizRepository;

    @Transactional(readOnly = true)
    public QuizResponse execute() {
        Quiz quiz = quizRepository.findRandomQuiz()
                .orElseThrow(() -> QuizNotFoundException.EXCEPTION);

        return QuizResponse.from(quiz);
    }
}

