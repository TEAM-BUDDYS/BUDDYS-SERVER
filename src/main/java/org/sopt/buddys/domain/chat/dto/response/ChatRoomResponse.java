package org.sopt.buddys.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import org.sopt.buddys.domain.chat.service.result.ChatRoomResult;
import org.sopt.buddys.domain.chat.util.ChatTimeConverter;

public record ChatRoomResponse(
    @Schema(description = "채팅방 ID", example = "1")
    Long chatRoomId,

    @Schema(description = "채팅방 생성 시각. UTC 기준입니다.", example = "2026-07-07T14:30:00Z")
    OffsetDateTime createdAt,

    @Schema(description = "상대방 사용자 정보")
    ChatParticipantResponse participant,

    @Schema(description = "상대방에게 메시지를 보낼 수 있는지 여부. "
        + "나 또는 상대방이 서로를 차단했거나 신고한 경우 false입니다.", example = "true")
    boolean canSendMessage
) {

  public static ChatRoomResponse from(ChatRoomResult result) {
    return new ChatRoomResponse(
        result.chatRoom().getId(),
        ChatTimeConverter.toCommonTime(result.chatRoom().getCreatedAt()),
        ChatParticipantResponse.from(result.participant()),
        result.canSendMessage()
    );
  }
}
