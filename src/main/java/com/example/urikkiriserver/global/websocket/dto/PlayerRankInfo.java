package com.example.urikkiriserver.global.websocket.dto;

import com.example.urikkiriserver.domain.user.domain.User;
import lombok.Builder;

@Builder
public record PlayerRankInfo(
        int rank,
        String nickname,
        int level,
        int bananaScore
) {
    public static PlayerRankInfo of(int rank, User user, int bananaScore) {
        return PlayerRankInfo.builder()
                .rank(rank)
                .nickname(user.getNickname())
                .level(user.getLevel())
                .bananaScore(bananaScore)
                .build();
    }
}