package com.example.urikkiriserver.domain.play.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class ParticipantNotFoundException extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new ParticipantNotFoundException();

    private ParticipantNotFoundException() {
        super(ErrorCode.PARTICIPANT_NOT_FOUND);
    }
}

