package org.sopt.buddys.domain.chat.service.result;

import java.time.LocalDateTime;
import java.util.List;

public record ChatRoomListResult(
    List<ChatRoomListItemResult> chatRooms,
    int page,
    int size,
    boolean hasNext
) {

  public ChatRoomListResult {
    chatRooms = List.copyOf(chatRooms);
  }

  public record ChatRoomListItemResult(
      Long chatRoomId,
      Long participantUserId,
      String participantNickname,
      String participantProfileImageUrl,
      String lastMessage,
      LocalDateTime lastMessageSentAt,
      long unreadMessageCount
  ) {
  }
}
