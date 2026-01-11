package com.example.urikkiriserver.global.websocket.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class WebSocketRoomNotFound extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new WebSocketRoomNotFound();

    private WebSocketRoomNotFound() {
        super(ErrorCode.WEBSOCKET_ROOM_NOT_FOUND);
    }
}

