package com.example.urikkiriserver.domain.play.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class AlreadyInRoomException extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new AlreadyInRoomException();

    private AlreadyInRoomException() {
        super(ErrorCode.ALREADY_IN_ROOM);
    }
}

