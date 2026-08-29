package org.sopt.buddys.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationSettingRequest(
    @Schema(description = "알림 설정 여부", example = "false")
    @NotNull Boolean notificationEnabled
) {
}
