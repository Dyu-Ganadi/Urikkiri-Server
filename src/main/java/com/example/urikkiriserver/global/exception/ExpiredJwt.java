package com.example.urikkiriserver.global.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class ExpiredJwt extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new ExpiredJwt();

    private ExpiredJwt() {
        super(ErrorCode.EXPIRED_JWT);
    }
}
