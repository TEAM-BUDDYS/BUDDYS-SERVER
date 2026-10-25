package org.sopt.buddys.domain.user.service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.sopt.buddys.domain.tag.entity.Tag;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.global.exception.BaseException;

public final class UserTagSelectionValidator {

  private static final int MIN_ACTIVITY_TAG_COUNT = 1;
  private static final int MAX_ACTIVITY_TAG_COUNT = 3;
  private static final int MIN_INTEREST_TAG_COUNT = 1;
  private static final int MAX_INTEREST_TAG_COUNT = 3;
  private static final int MIN_TRAVEL_STYLE_TAG_COUNT = 1;
  private static final int MAX_TRAVEL_STYLE_TAG_COUNT = 5;

  private UserTagSelectionValidator() {
  }

  public static void validate(Collection<Tag> tags) {
    Map<TagType, Long> tagCounts = tags.stream()
        .collect(Collectors.groupingBy(Tag::getTagType, Collectors.counting()));
    if (!isBetween(tagCounts.getOrDefault(TagType.ACTIVITY, 0L),
        MIN_ACTIVITY_TAG_COUNT, MAX_ACTIVITY_TAG_COUNT)
        || !isBetween(tagCounts.getOrDefault(TagType.INTEREST, 0L),
        MIN_INTEREST_TAG_COUNT, MAX_INTEREST_TAG_COUNT)
        || !isBetween(tagCounts.getOrDefault(TagType.TRAVEL_STYLE, 0L),
        MIN_TRAVEL_STYLE_TAG_COUNT, MAX_TRAVEL_STYLE_TAG_COUNT)) {
      throw new BaseException(UserErrorCode.INVALID_TAG_SELECTION_COUNT);
    }
  }

  private static boolean isBetween(long value, int minimum, int maximum) {
    return value >= minimum && value <= maximum;
  }
}
