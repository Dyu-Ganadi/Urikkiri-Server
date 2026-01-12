package com.example.urikkiriserver.domain.user.service;

import com.example.urikkiriserver.domain.user.domain.User;
import com.example.urikkiriserver.domain.user.domain.repository.UserRepository;
import com.example.urikkiriserver.domain.user.presentation.dto.response.*;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TotalRankingService {

    private final UserRepository userRepository;

    public RankingScrollResponse getRankingScroll(
            Integer lastBananaxp,
            Long lastUserId,
            int size
    ) {
        Pageable pageable = PageRequest.of(0, size + 1);

        List<User> users;

        // 첫 요청
        if (lastBananaxp == null || lastUserId == null) {
            users = userRepository.findFirstRankings(pageable);
        }
        // 다음 스크롤
        else {
            users = userRepository.findNextRankings(
                    lastBananaxp,
                    lastUserId,
                    pageable
            );
        }

        boolean hasNext = users.size() > size;
        if (hasNext) {
            users = users.subList(0, size);
        }

        List<RankingItemResponse> items = users.stream()
                .map(user -> RankingItemResponse.of(
                        user.getNickname(),
                        user.getBananaxp(),
                        user.getLevel()
                ))
                .toList();

        RankingCursorResponse nextCursor = users.isEmpty()
                ? null
                : RankingCursorResponse.of(
                users.get(users.size() - 1).getBananaxp(),
                users.get(users.size() - 1).getId()
        );

        return RankingScrollResponse.of(
                items,
                nextCursor,
                hasNext
        );
    }
}
