package org.sopt.buddys.domain.auth.dto.response;

public record AuthTokens(Long userId, String accessToken, String refreshToken, boolean onboardingCompleted) {}
