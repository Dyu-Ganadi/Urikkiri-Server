package com.example.urikkiriserver.global.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class InvalidJwt extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new InvalidJwt();

    private InvalidJwt() {
        super(ErrorCode.INVALID_JWT);
    }
}
