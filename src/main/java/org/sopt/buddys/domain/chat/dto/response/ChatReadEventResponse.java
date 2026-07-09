package org.sopt.buddys.domain.chat.dto.response;

import org.sopt.buddys.domain.chat.service.result.ChatReadResult;

public record ChatReadEventResponse(
    String type,
    Long chatRoomId,
    Long readerId,
    Long lastReadMessageId
) {

  private static final String READ_EVENT_TYPE = "READ";

  public static ChatReadEventResponse from(ChatReadResult result) {
    return new ChatReadEventResponse(
        READ_EVENT_TYPE,
        result.chatRoomId(),
        result.readerId(),
        result.lastReadMessageId()
    );
  }
}
