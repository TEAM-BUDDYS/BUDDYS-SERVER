package org.sopt.buddys.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimeConverterTest {

  @DisplayName("UTC로 저장된 LocalDateTime을 UTC OffsetDateTime으로 변환한다")
  @Test
  void toCommonTime_interpretsStorageTimeAsUtc() {
    LocalDateTime storageTime = LocalDateTime.of(2026, 8, 30, 14, 20);

    OffsetDateTime commonTime = TimeConverter.toCommonTime(storageTime);

    assertThat(commonTime)
        .isEqualTo(OffsetDateTime.of(storageTime, ZoneOffset.UTC));
  }

  @DisplayName("오프셋이 있는 시각을 UTC 저장 시각으로 변환한다")
  @Test
  void toStorageTime_convertsToUtc() {
    OffsetDateTime seoulTime = OffsetDateTime.parse("2026-08-30T14:20:00+09:00");

    LocalDateTime storageTime = TimeConverter.toStorageTime(seoulTime);

    assertThat(storageTime).isEqualTo(LocalDateTime.of(2026, 8, 30, 5, 20));
  }

  @DisplayName("변환할 시각이 없으면 null을 반환한다")
  @Test
  void conversion_returnsNullForNull() {
    assertThat(TimeConverter.toCommonTime(null)).isNull();
    assertThat(TimeConverter.toStorageTime(null)).isNull();
  }
}
