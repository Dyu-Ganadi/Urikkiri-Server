package com.example.urikkiriserver.domain.user.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class UserExists extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new UserExists();

    private UserExists() {
        super(ErrorCode.USER_EXISTS);
    }
}
