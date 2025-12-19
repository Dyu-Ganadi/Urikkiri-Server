package com.example.urikkiriserver.domain.user.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
    @Email
    @Size(max = 30)
    @NotBlank
    String email,

    @NotBlank
    @Size(max = 8)
    String nickname,

    @NotBlank
    String password
) {
}
