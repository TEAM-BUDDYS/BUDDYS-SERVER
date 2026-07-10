package org.sopt.buddys.domain.chat.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class ChatTimeConverter {

  private static final ZoneOffset COMMON_OFFSET = ZoneOffset.UTC;

  private ChatTimeConverter() {
  }

  public static LocalDateTime now() {
    return LocalDateTime.now(COMMON_OFFSET);
  }

  public static OffsetDateTime toCommonTime(LocalDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }

    return dateTime.atOffset(COMMON_OFFSET);
  }

  public static LocalDateTime toStorageTime(OffsetDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }

    return dateTime.withOffsetSameInstant(COMMON_OFFSET)
        .toLocalDateTime();
  }
}
