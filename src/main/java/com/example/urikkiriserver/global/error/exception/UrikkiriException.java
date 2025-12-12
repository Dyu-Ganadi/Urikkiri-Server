package com.example.urikkiriserver.global.error.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UrikkiriException extends RuntimeException {

    private final ErrorCode errorCode;
}
