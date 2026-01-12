package com.example.urikkiriserver.domain.user.service;

import com.example.urikkiriserver.domain.user.domain.repository.UserRepository;
import com.example.urikkiriserver.domain.user.presentation.dto.response.UserRankingResponse;
import com.example.urikkiriserver.domain.user.presentation.dto.response.UserRankingResponseElement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TotalRankingService {

    private final UserRepository userRepository;

    public UserRankingResponse getAllUserRanking() {
        List<UserRankingResponseElement> rankings =
                userRepository.findAllByOrderByBananaxpDesc()
                        .stream()
                        .map(user -> UserRankingResponseElement.of(
                                user.getLevel(),
                                user.getNickname(),
                                user.getBananaxp()
                        ))
                        .toList();

        return new UserRankingResponse(rankings);
    }
}
