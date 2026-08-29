package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.user.entity.User;

public record NotificationSettingResponse(
    @Schema(description = "알림 설정 여부", example = "true")
    boolean notificationEnabled
) {

  public static NotificationSettingResponse from(User user) {
    return new NotificationSettingResponse(user.isNotificationEnabled());
  }
}
