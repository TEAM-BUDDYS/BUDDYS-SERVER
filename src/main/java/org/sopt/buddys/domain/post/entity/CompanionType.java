package org.sopt.buddys.domain.post.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CompanionType {
  FULL_TRIP,
  PARTIAL_TRIP,
  ACCOMMODATION_SHARE,
  TOUR,
  MEAL,
  DAILY_LIFE,
  GROUP_PURCHASE;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static CompanionType fromJson(String name) {
    return valueOf(name);
  }
}
