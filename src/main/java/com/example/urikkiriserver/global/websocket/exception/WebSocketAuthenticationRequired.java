package com.example.urikkiriserver.global.websocket.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class WebSocketAuthenticationRequired extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new WebSocketAuthenticationRequired();

    private WebSocketAuthenticationRequired() {
        super(ErrorCode.WEBSOCKET_AUTHENTICATION_REQUIRED);
    }
}

