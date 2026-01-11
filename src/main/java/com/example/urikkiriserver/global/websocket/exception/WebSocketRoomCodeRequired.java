package com.example.urikkiriserver.global.websocket.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class WebSocketRoomCodeRequired extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new WebSocketRoomCodeRequired();

    private WebSocketRoomCodeRequired() {
        super(ErrorCode.WEBSOCKET_ROOM_CODE_REQUIRED);
    }
}

