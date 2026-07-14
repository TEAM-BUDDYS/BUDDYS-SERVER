package org.sopt.buddys.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
    @Schema(description = "사용자 ID", example = "1") Long userId,
    @Schema(description = "API 인증에 사용하는 Access Token") String accessToken,
    @Schema(description = "온보딩(성별/생년월일/태그 3개) 완료 여부") boolean onboardingCompleted
) {}
