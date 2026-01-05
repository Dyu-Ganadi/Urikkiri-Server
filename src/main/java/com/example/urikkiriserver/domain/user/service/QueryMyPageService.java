package com.example.urikkiriserver.domain.user.service;

import com.example.urikkiriserver.domain.user.domain.User;
import com.example.urikkiriserver.domain.user.presentation.dto.response.MyPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueryMyPageService {

    private final UserFacade userFacade;

    public MyPageResponse execute() {
        User user = userFacade.getCurrentUser();
        return MyPageResponse.of(user);
    }
}
