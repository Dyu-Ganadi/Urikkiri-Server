package com.example.urikkiriserver.domain.play.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class ExaminerNotFoundException extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new ExaminerNotFoundException();

    private ExaminerNotFoundException() {
        super(ErrorCode.EXAMINER_NOT_FOUND);
    }
}

