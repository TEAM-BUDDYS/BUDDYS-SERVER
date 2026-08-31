package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationSettingResponse(
    @Schema(
            description = "알림 설정 여부",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean notificationEnabled
) {

  public static NotificationSettingResponse of(boolean notificationEnabled) {
    return new NotificationSettingResponse(notificationEnabled);
  }
}
