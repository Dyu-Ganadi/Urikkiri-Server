package com.example.urikkiriserver.domain.user.presentation.dto.response;

public record RankingCursorResponse(
        Integer lastBananaxp,
        Long lastUserId
) {
    public static RankingCursorResponse of(
            Integer lastBananaxp,
            Long lastUserId
    ) {
        return new RankingCursorResponse(lastBananaxp, lastUserId);
    }
}
