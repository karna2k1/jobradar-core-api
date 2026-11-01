package com.jobradar.core.auth.dto;

public record AuthResponse(String accessToken, String refreshToken, long expiresIn) {
}
