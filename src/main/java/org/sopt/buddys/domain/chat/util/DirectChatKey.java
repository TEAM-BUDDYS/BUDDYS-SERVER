package org.sopt.buddys.domain.chat.util;

public final class DirectChatKey {

  private DirectChatKey() {
  }

  public static String from(
      Long userId,
      Long participantUserId
  ) {

    long firstUserId = Math.min(userId, participantUserId);
    long secondUserId = Math.max(userId, participantUserId);
    return "%d:%d".formatted(firstUserId, secondUserId);
  }
}
