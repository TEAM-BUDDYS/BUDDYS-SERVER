package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.user.entity.User;

public record OnboardingResponse(
    @Schema(description = "사용자 ID", example = "1") Long userId,
    @Schema(description = "사용자 닉네임", example = "해령") String nickname
) {
  public static OnboardingResponse of(User user) {
    return new OnboardingResponse(user.getId(), user.getNickname());
  }
}