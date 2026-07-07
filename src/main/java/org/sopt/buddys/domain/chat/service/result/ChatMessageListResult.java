package org.sopt.buddys.domain.chat.service.result;

import java.time.LocalDateTime;
import java.util.List;
import org.sopt.buddys.domain.chat.entity.ChatMessage;

public record ChatMessageListResult(
    List<ChatMessageResult> messages,
    LocalDateTime nextCursorSentAt,
    Long nextCursorMessageId,
    boolean hasNext
) {

  public ChatMessageListResult {
    messages = List.copyOf(messages);
  }

  public record ChatMessageResult(
      ChatMessage message,
      boolean mine
  ) {
  }
}
