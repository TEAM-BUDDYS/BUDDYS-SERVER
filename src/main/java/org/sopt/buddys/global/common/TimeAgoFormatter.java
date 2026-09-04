package org.sopt.buddys.global.common;

import java.time.Duration;
import java.time.LocalDateTime;

public final class TimeAgoFormatter {

  private TimeAgoFormatter() {
  }

  public static String format(LocalDateTime createdAt, LocalDateTime now) {
    long seconds = Math.max(0, Duration.between(createdAt, now).getSeconds());
    if (seconds < 60) {
      return "방금 전";
    }

    long minutes = seconds / 60;
    if (minutes < 60) {
      return "%d분 전".formatted(minutes);
    }

    long hours = minutes / 60;
    if (hours < 24) {
      return "%d시간 전".formatted(hours);
    }

    long days = hours / 24;
    if (days < 30) {
      return "%d일 전".formatted(days);
    }

    if (days < 365) {
      return "%d개월 전".formatted(Math.min(days / 30, 11));
    }

    return "%d년 전".formatted(days / 365);
  }
}
