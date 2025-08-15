package com.maxxenergy.auth;

public record AuthResponse(
        String token,
        String username,
        String role
) {}
