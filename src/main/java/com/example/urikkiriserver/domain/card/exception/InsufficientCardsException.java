package com.example.urikkiriserver.domain.card.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class InsufficientCardsException extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new InsufficientCardsException();

    private InsufficientCardsException() {
        super(ErrorCode.INSUFFICIENT_CARDS);
    }
}

