package com.example.urikkiriserver.domain.play.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class RoomNotFoundException extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new RoomNotFoundException();

    private RoomNotFoundException() {
        super(ErrorCode.ROOM_NOT_FOUND);
    }
}
