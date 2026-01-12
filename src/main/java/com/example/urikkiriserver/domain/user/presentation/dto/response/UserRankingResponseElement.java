package com.example.urikkiriserver.domain.user.presentation.dto.response;

public record UserRankingResponseElement(
        Integer level,
        String nickname,
        Integer bananaxp
) {
    public static UserRankingResponseElement of(Integer level, String nickname, Integer bananaxp) {
        return new UserRankingResponseElement(level, nickname, bananaxp);
    }
}
