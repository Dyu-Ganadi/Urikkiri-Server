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
    INVALID_JWT(HttpStatus.UNAUTHORIZED, "Invalid JWT");

    private final HttpStatus status;
    private final String message;
}
