package org.sopt.buddys.global.common;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** UTC {@link LocalDateTime} 저장값과 오프셋이 포함된 API 시각 사이의 변환을 담당한다. */
public final class TimeConverter {

  private static final ZoneOffset STORAGE_OFFSET = ZoneOffset.UTC;

  private TimeConverter() {
  }

  public static LocalDateTime now() {
    return LocalDateTime.now(STORAGE_OFFSET);
  }

  public static OffsetDateTime toCommonTime(LocalDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }

    return dateTime.atOffset(STORAGE_OFFSET);
  }

  public static LocalDateTime toStorageTime(OffsetDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }

    return dateTime.withOffsetSameInstant(STORAGE_OFFSET)
        .toLocalDateTime();
  }
}
