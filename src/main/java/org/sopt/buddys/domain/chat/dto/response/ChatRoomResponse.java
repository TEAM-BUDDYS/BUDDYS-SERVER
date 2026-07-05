package org.sopt.buddys.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.chat.service.result.ChatRoomResult;
import org.sopt.buddys.domain.user.entity.User;

public record ChatRoomResponse(
    @Schema(description = "채팅방 ID", example = "1")
    Long chatRoomId,

    @Schema(description = "상대방 사용자 정보")
    ParticipantResponse participant
) {

  public static ChatRoomResponse from(ChatRoomResult result) {
    return new ChatRoomResponse(
        result.chatRoom().getId(),
        ParticipantResponse.from(result.participant())
    );
  }

  public record ParticipantResponse(
      @Schema(description = "상대방 사용자 ID", example = "20")
      Long userId,

      @Schema(description = "상대방 닉네임", example = "민지")
      String nickname,

      @Schema(description = "상대방 프로필 이미지 URL", example = "https://example.com/profile.png")
      String profileImageUrl
  ) {

    private static ParticipantResponse from(User participant) {
      return new ParticipantResponse(
          participant.getId(),
          participant.getNickname(),
          participant.getProfileImageUrl()
      );
    }
  }
}
