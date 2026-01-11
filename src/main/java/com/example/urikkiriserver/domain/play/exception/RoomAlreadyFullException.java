package com.example.urikkiriserver.domain.play.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class RoomAlreadyFullException extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new RoomAlreadyFullException();

    private RoomAlreadyFullException() {
        super(ErrorCode.ROOM_ALREADY_FULL);
    }
}
