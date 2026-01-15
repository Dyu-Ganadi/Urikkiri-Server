package com.example.urikkiriserver.global.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String parseToken = jwtProvider.resolveToken(request);

        if (parseToken != null) {
            try {
                Authentication authentication = jwtProvider.authentication(parseToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // JWT 검증 실패 시 로그를 남기고 인증 없이 진행 (ExceptionFilter에서 처리)
                log.warn("JWT authentication failed for request: {} - Error: {}",
                    request.getRequestURI(), e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}