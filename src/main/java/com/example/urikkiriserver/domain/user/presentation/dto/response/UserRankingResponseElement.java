package com.example.urikkiriserver.domain.user.presentation.dto.response;

public record UserRankingResponseElement(
        Integer level,
        String nickname,
        Long bananaxp
) {
    public static UserRankingResponseElement of(Integer level, String nickname, Long bananaxp) {
        return new UserRankingResponseElement(level, nickname, bananaxp);
    }
}
