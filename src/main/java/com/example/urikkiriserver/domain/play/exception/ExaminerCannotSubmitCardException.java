package com.example.urikkiriserver.domain.play.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class ExaminerCannotSubmitCardException extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new ExaminerCannotSubmitCardException();

    private ExaminerCannotSubmitCardException() {
        super(ErrorCode.EXAMINER_CANNOT_SUBMIT_CARD);
    }
}

