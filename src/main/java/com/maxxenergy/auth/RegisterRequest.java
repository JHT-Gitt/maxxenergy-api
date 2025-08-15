package com.maxxenergy.auth;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank String username,
        @Email @NotBlank String email,
        @Size(min = 6) String password
) {}
