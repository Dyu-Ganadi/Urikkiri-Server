package com.example.urikkiriserver.global.websocket.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class WebSocketNotParticipant extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new WebSocketNotParticipant();

    private WebSocketNotParticipant() {
        super(ErrorCode.WEBSOCKET_NOT_PARTICIPANT);
    }
}

