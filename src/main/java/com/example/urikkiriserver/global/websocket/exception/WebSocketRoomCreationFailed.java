package com.example.urikkiriserver.global.websocket.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class WebSocketRoomCreationFailed extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new WebSocketRoomCreationFailed();

    private WebSocketRoomCreationFailed() {
        super(ErrorCode.WEBSOCKET_ROOM_CREATION_FAILED);
    }
}

