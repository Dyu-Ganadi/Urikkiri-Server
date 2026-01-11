package com.example.urikkiriserver.global.websocket.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class WebSocketInvalidMessageFormat extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new WebSocketInvalidMessageFormat();

    private WebSocketInvalidMessageFormat() {
        super(ErrorCode.WEBSOCKET_INVALID_MESSAGE_FORMAT);
    }
}

