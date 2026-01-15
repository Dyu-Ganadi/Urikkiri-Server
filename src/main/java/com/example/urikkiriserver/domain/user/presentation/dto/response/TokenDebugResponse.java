package com.example.urikkiriserver.domain.user.presentation.dto.response;

public record TokenDebugResponse(
        String message,
        String email,
        Boolean authenticated,
        String errorDetail
) {
    public static TokenDebugResponse success(String email) {
        return new TokenDebugResponse(
                "Token is valid",
                email,
                true,
                null
        );
    }

    public static TokenDebugResponse failure(String errorDetail) {
        return new TokenDebugResponse(
                "Token validation failed",
                null,
                false,
                errorDetail
        );
    }
}

