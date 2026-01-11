package com.example.urikkiriserver.domain.quiz.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class QuizNotFoundException extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new QuizNotFoundException();

    private QuizNotFoundException() {
        super(ErrorCode.QUIZ_NOT_FOUND);
    }
}

