package com.example.urikkiriserver.domain.card.exception;

import com.example.urikkiriserver.global.error.exception.ErrorCode;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;

public class CardNotFoundException extends UrikkiriException {

    public static final UrikkiriException EXCEPTION = new CardNotFoundException();

    private CardNotFoundException() {
        super(ErrorCode.CARD_NOT_FOUND);
    }
}

