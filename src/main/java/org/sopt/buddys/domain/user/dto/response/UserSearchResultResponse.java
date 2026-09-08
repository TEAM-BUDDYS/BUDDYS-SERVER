package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.user.entity.User;

public record UserSearchResultResponse(
    @Schema(description = "사용자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long userId,

    @Schema(description = "닉네임", example = "버디", requiredMode = Schema.RequiredMode.REQUIRED)
    String nickname,

    @Schema(
        description = "프로필 이미지 URL",
        example = "https://example.com/profile.png",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String profileImageUrl
) {

  public static UserSearchResultResponse from(User user) {
    return new UserSearchResultResponse(user.getId(), user.getNickname(), user.getProfileImageUrl());
  }
}
