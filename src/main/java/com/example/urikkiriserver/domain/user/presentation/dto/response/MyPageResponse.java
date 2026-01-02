package com.example.urikkiriserver.domain.user.presentation.dto.response;

import com.example.urikkiriserver.domain.user.domain.User;

public record MyPageResponse(
        Long id,
        String email,
        String nickname,
        Integer level,
        Integer bananaxp
) {
    public static MyPageResponse of(User user) {
        return new MyPageResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getLevel(),
                user.getBananaxp()
        );
    }
}

