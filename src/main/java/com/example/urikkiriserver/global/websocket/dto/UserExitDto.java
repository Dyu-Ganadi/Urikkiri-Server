package com.example.urikkiriserver.global.websocket.dto;

public record UserExitDto(
        Long userId,
        String nickname,
        Integer remainingCount
) {
    public static UserExitDto of(Long userId, String nickname, Integer remainingCount) {
        return new UserExitDto(userId, nickname, remainingCount);
    }
}