package com.example.urikkiriserver.domain.user.presentation.dto.response;

import java.util.List;

public record RankingScrollResponse(
        List<RankingItemResponse> items,
        RankingCursorResponse nextCursor,
        boolean hasNext
) {
    public static RankingScrollResponse of(
            List<RankingItemResponse> items,
            RankingCursorResponse nextCursor,
            boolean hasNext
    ) {
        return new RankingScrollResponse(items, nextCursor, hasNext);
    }
}
