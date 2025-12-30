package com.example.urikkiriserver.global.security.jwt;

import com.example.urikkiriserver.global.exception.ExpiredJwt;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.example.urikkiriserver.global.error.exception.ErrorCode.EXPIRED_JWT;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String parseToken = jwtProvider.resolveToken(request);

        if (parseToken != null) {
            // 블랙리스트 검사
            if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + parseToken))) {
                throw ExpiredJwt.EXCEPTION;
            }

            Authentication authentication = jwtProvider.authentication(parseToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}