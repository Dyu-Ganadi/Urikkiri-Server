package com.example.urikkiriserver.domain.user.service;

import com.example.urikkiriserver.domain.user.presentation.dto.request.LoginRequest;
import com.example.urikkiriserver.domain.user.domain.User;
import com.example.urikkiriserver.domain.user.domain.repository.UserRepository;
import com.example.urikkiriserver.domain.user.exception.PasswordMisMatch;
import com.example.urikkiriserver.domain.user.exception.UserNotFound;
import com.example.urikkiriserver.domain.user.presentation.dto.response.TokenResponse;
import com.example.urikkiriserver.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public TokenResponse execute(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> UserNotFound.EXCEPTION);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw PasswordMisMatch.EXCEPTION;
        }

        return jwtProvider.createToken(user.getEmail());
    }
}
