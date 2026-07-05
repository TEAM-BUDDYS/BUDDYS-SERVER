package org.sopt.buddys.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.user.entity.User;

public record ChatParticipantResponse(
    @Schema(description = "상대방 사용자 ID", example = "2")
    Long userId,

    @Schema(description = "상대방 닉네임", example = "민지")
    String nickname,

    @Schema(description = "상대방 프로필 이미지 URL", example = "https://example.com/profile.png")
    String profileImageUrl
) {

  public static ChatParticipantResponse from(User participant) {
    return new ChatParticipantResponse(
        participant.getId(),
        participant.getNickname(),
        participant.getProfileImageUrl()
    );
  }
}
