package com.example.urikkiriserver.domain.user.service;

import com.example.urikkiriserver.domain.user.domain.User;
import com.example.urikkiriserver.domain.user.domain.repository.UserRepository;
import com.example.urikkiriserver.domain.user.exception.UserNotFound;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> UserNotFound.EXCEPTION);
    }
}

