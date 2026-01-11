package com.example.urikkiriserver.global.error.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"),

    // user
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User Not Found"),
    USER_EXISTS(HttpStatus.CONFLICT, "User Already Exists"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "Password Mismatch"),

    // jwt
    EXPIRED_JWT(HttpStatus.UNAUTHORIZED, "Expired JWT"),
    INVALID_JWT(HttpStatus.UNAUTHORIZED, "Invalid JWT"),

    // websocket
    WEBSOCKET_AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "WebSocket Authentication Required"),
    WEBSOCKET_INVALID_MESSAGE_FORMAT(HttpStatus.BAD_REQUEST, "Invalid WebSocket Message Format"),
    WEBSOCKET_ROOM_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "Room Code is Required"),
    WEBSOCKET_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Room Not Found"),
    WEBSOCKET_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "You are not a participant of this room"),
    WEBSOCKET_ROOM_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to Create Room"),

    // play - room
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Room Not Found"),
    ROOM_ALREADY_FULL(HttpStatus.BAD_REQUEST, "Room is Already Full"),
    ALREADY_IN_ROOM(HttpStatus.CONFLICT, "User is Already in This Room"),

    // quiz
    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "Quiz Not Found"),

    // card
    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "Card Not Found");

    private final HttpStatus status;
    private final String message;
}
