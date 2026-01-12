package com.example.urikkiriserver.domain.user.presentation.dto.response;

import java.util.List;

public record UserRankingResponse(
        List<UserRankingResponseElement> rankings
) {
}
