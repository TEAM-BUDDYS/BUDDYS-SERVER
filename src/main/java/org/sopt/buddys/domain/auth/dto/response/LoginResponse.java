package org.sopt.buddys.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
    @Schema(description = "API 인증에 사용하는 Access Token") String accessToken,
    @Schema(description = "신규 회원 여부") boolean isNewUser
) {}
