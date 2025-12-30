package com.example.urikkiriserver.domain.user.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
    @Email
    @Size(max = 254)
    @NotBlank
    String email,

    @NotBlank
    @Size(max = 15)
    String nickname,

    @NotBlank
    @Size(min=12, max = 64)
    String password
) {
}
