package org.sopt.buddys.domain.chat.dto.response;

import java.time.LocalDateTime;
import org.sopt.buddys.domain.chat.entity.ChatMessage;
import org.sopt.buddys.domain.chat.service.result.ChatMessageSendResult;

public record ChatMessageEventResponse(
    String type,
    Long chatRoomId,
    MessageResponse message
) {

  private static final String MESSAGE_EVENT_TYPE = "MESSAGE";

  public static ChatMessageEventResponse from(ChatMessageSendResult result) {
    ChatMessage message = result.message();

    return new ChatMessageEventResponse(
        MESSAGE_EVENT_TYPE,
        message.getChatRoom().getId(),
        MessageResponse.from(message)
    );
  }

  public record MessageResponse(
      Long messageId,
      ChatParticipantResponse sender,
      String content,
      LocalDateTime sentAt
  ) {

    private static MessageResponse from(ChatMessage message) {
      return new MessageResponse(
          message.getId(),
          ChatParticipantResponse.from(message.getSender()),
          message.getMessage(),
          message.getCreatedAt()
      );
    }
  }
}
