package com.example.urikkiriserver.domain.user.service;

import com.example.urikkiriserver.global.exception.ExpiredJwt;
import com.example.urikkiriserver.global.exception.InvalidJwt;
import com.example.urikkiriserver.global.security.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    public void execute(HttpServletRequest request) {
        String accessToken = jwtProvider.resolveToken(request);

        if(accessToken == null) {
            throw InvalidJwt.EXCEPTION;
        }

        long expirationTime = jwtProvider.getExpiration(accessToken).getTime();
        long now = System.currentTimeMillis();

        long ttl = expirationTime - now;

        if (ttl <= 0) {
            return;
        }

        redisTemplate.opsForValue().set(
                "blacklist:" + accessToken,
                "logout",
                ttl,
                TimeUnit.MILLISECONDS
        );
    }

}

