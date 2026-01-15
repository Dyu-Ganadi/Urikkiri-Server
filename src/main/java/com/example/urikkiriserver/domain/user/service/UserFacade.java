package com.example.urikkiriserver.domain.user.service;

import com.example.urikkiriserver.domain.user.domain.User;
import com.example.urikkiriserver.domain.user.domain.repository.UserRepository;
import com.example.urikkiriserver.domain.user.exception.UserNotFound;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw UserNotFound.EXCEPTION;
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> UserNotFound.EXCEPTION);
    }
}

