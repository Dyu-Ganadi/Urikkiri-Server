package com.example.urikkiriserver.domain.user.presentation;

import com.example.urikkiriserver.domain.user.presentation.dto.request.LoginRequest;
import com.example.urikkiriserver.domain.user.presentation.dto.request.SignUpRequest;
import com.example.urikkiriserver.domain.user.presentation.dto.response.MyPageResponse;
import com.example.urikkiriserver.domain.user.presentation.dto.response.TokenDebugResponse;
import com.example.urikkiriserver.domain.user.presentation.dto.response.TokenResponse;
import com.example.urikkiriserver.domain.user.presentation.dto.response.UserRankingResponse;
import com.example.urikkiriserver.domain.user.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final SignUpService signUpService;
    private final LoginService loginService;
    private final LogoutService logoutService;
    private final QueryMyPageService queryMyPageService;
    private final TotalRankingService totalRankingService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse signup(@RequestBody @Valid SignUpRequest request) {
        return signUpService.execute(request);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse login(@RequestBody @Valid LoginRequest request) {
        return loginService.execute(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        logoutService.execute(request);
    }

    @GetMapping("/my")
    @ResponseStatus(HttpStatus.OK)
    public MyPageResponse getMyPage() {
        return queryMyPageService.execute();
    }

    @GetMapping("/who-is-the-king")
    public UserRankingResponse getUserRanking() {
        return totalRankingService.getAllUserRanking();
    }

    // 디버깅용 엔드포인트 - 배포 환경에서 JWT 토큰 검증 테스트
    @GetMapping("/debug/token")
    @ResponseStatus(HttpStatus.OK)
    public TokenDebugResponse debugToken() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null) {
                log.warn("Authentication is null");
                return TokenDebugResponse.failure("Authentication is null");
            }

            if (!authentication.isAuthenticated()) {
                log.warn("Authentication is not authenticated");
                return TokenDebugResponse.failure("Not authenticated");
            }

            String email = authentication.getName();
            if ("anonymousUser".equals(email)) {
                log.warn("User is anonymous");
                return TokenDebugResponse.failure("Anonymous user");
            }

            log.info("Token debug success for email: {}", email);
            return TokenDebugResponse.success(email);
        } catch (Exception e) {
            log.error("Token debug failed", e);
            return TokenDebugResponse.failure(e.getMessage());
        }
    }
}
