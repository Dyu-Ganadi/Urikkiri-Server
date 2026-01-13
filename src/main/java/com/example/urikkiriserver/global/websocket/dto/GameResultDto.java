package com.example.urikkiriserver.global.websocket.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record GameResultDto(
        List<PlayerRankInfo> rankings
) {
    public static GameResultDto of(List<PlayerRankInfo> rankings) {
        return GameResultDto.builder()
                .rankings(rankings)
                .build();
    }
}