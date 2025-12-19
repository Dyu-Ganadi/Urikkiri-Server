package com.example.urikkiriserver.domain.user.presentation;

import com.example.urikkiriserver.domain.user.presentation.dto.request.SignUpRequest;
import com.example.urikkiriserver.domain.user.presentation.dto.response.TokenResponse;
import com.example.urikkiriserver.domain.user.service.SignUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final SignUpService signUpService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse signup(@RequestBody @Valid SignUpRequest request) {
        return signUpService.execute(request);
    }
}
