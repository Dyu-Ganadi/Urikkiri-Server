package com.example.urikkiriserver.domain.user.service;

import com.example.urikkiriserver.domain.user.domain.User;
import com.example.urikkiriserver.domain.user.domain.repository.UserRepository;
import com.example.urikkiriserver.domain.user.exception.UserExists;
import com.example.urikkiriserver.domain.user.presentation.dto.request.SignUpRequest;
import com.example.urikkiriserver.domain.user.presentation.dto.response.TokenResponse;
import com.example.urikkiriserver.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignUpService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public TokenResponse execute(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw UserExists.EXCEPTION;
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw UserExists.EXCEPTION;
        }

        userRepository.save(
            User.builder()
                .email(request.email())
                .nickname(request.nickname())
                .password(passwordEncoder.encode(request.password()))
                .bananaxp(0)
                .level(1)
                .build()
        );

        return jwtProvider.createToken(request.email());
    }
}
