package com.example.urikkiriserver.domain.user.presentation.dto.response;

public record RankingItemResponse(
        String nickname,
        Integer bananaxp,
        Integer level
) {
    public static RankingItemResponse of(
            String nickname,
            Integer bananaxp,
            Integer level
    ) {
        return new RankingItemResponse(nickname, bananaxp, level);
    }
}
