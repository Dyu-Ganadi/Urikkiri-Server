package com.example.urikkiriserver.domain.user.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class PasswordMisMatch extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new PasswordMisMatch();

    private PasswordMisMatch() {
        super(ErrorCode.PASSWORD_MISMATCH);
    }
}
