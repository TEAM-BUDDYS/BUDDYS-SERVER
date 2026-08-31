package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record NicknameAvailabilityResponse(
    @Schema(
        description = "사용 가능한 닉네임인지 여부",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean available
) {}
