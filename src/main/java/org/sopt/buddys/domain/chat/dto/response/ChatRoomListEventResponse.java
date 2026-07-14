package org.sopt.buddys.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.chat.dto.response.ChatRoomListResponse.ChatRoomListItemResponse;
import org.sopt.buddys.domain.chat.service.result.ChatRoomListResult.ChatRoomListItemResult;

public record ChatRoomListEventResponse(
    @Schema(description = "이벤트 타입", example = "CHAT_ROOM_UPDATED")
    String type,

    @Schema(description = "갱신된 채팅방 목록 아이템")
    ChatRoomListItemResponse chatRoom
) {

  private static final String CHAT_ROOM_UPDATED = "CHAT_ROOM_UPDATED";

  public static ChatRoomListEventResponse from(ChatRoomListItemResult result) {
    return new ChatRoomListEventResponse(
        CHAT_ROOM_UPDATED,
        ChatRoomListItemResponse.from(result)
    );
  }
}
