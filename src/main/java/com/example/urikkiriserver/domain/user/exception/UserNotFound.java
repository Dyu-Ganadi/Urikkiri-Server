package com.example.urikkiriserver.domain.user.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class UserNotFound extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new UserNotFound();

    private UserNotFound() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}
