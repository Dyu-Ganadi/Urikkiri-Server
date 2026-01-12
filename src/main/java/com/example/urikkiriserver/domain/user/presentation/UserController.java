package com.example.urikkiriserver.domain.user.presentation;

import com.example.urikkiriserver.domain.user.presentation.dto.request.LoginRequest;
import com.example.urikkiriserver.domain.user.presentation.dto.request.SignUpRequest;
import com.example.urikkiriserver.domain.user.presentation.dto.response.MyPageResponse;
import com.example.urikkiriserver.domain.user.presentation.dto.response.RankingScrollResponse;
import com.example.urikkiriserver.domain.user.presentation.dto.response.TokenResponse;
import com.example.urikkiriserver.domain.user.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final SignUpService signUpService;
    private final LoginService loginService;
    private final LogoutService logoutService;
    private final QueryMyPageService queryMyPageService;
    private final TotalRankingService rankingService;

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


    @GetMapping("/users/ranking")
    public RankingScrollResponse getRankingScroll(
            @RequestParam(required = false) Integer lastBananaxp,
            @RequestParam(required = false) Long lastUserId,
            @RequestParam(defaultValue = "20") int size
    ) {
        return rankingService.getRankingScroll(
                lastBananaxp,
                lastUserId,
                size
        );
    }
}
