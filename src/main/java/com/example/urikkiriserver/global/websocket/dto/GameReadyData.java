package com.example.urikkiriserver.global.websocket.dto;

import java.util.List;

/**
 * 게임 준비 완료 데이터
 * 프론트에게 4명이 모였음을 알림
 */
public record GameReadyData(
        List<ParticipantInfo> participants,
        String message
) {
    public static GameReadyData of(List<ParticipantInfo> participants) {
        return new GameReadyData(
                participants,
                "All players ready. Launch Unity game with your token and room code."
        );
    }
}

