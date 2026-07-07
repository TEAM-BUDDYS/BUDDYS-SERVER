package org.sopt.buddys.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.chat.service.result.ChatRoomResult;

public record ChatRoomResponse(
    @Schema(description = "채팅방 ID", example = "1")
    Long chatRoomId,

    @Schema(description = "상대방 사용자 정보")
    ChatParticipantResponse participant
) {

  public static ChatRoomResponse from(ChatRoomResult result) {
    return new ChatRoomResponse(
        result.chatRoom().getId(),
        ChatParticipantResponse.from(result.participant())
    );
  }
}
