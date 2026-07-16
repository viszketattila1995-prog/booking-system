package com.attila.bookingsystem.dto.auth;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        String email,
        String fullName,
        List<String> roles
) {
}
