package com.example.urikkiriserver.domain.user.presentation.dto.response;

import java.time.LocalDateTime;

public record TokenResponse(
    String accessToken,
    LocalDateTime accessExp
) {
    public static TokenResponse of(String accessToken, LocalDateTime accessExp) {
        return new TokenResponse(accessToken, accessExp);
    }
}
