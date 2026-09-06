package org.sopt.buddys.domain.tag.service;

import java.util.List;
import org.sopt.buddys.domain.tag.entity.Tag;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.sopt.buddys.global.exception.BaseException;

public final class TagTypeCountValidator {

  private static final int MAX_ACTIVITY_TAG_COUNT = 3;
  private static final int MAX_INTEREST_TAG_COUNT = 2;
  private static final int MAX_TRAVEL_STYLE_TAG_COUNT = 2;

  private TagTypeCountValidator() {
  }

  public static void validate(List<Tag> tags, ErrorCode activityTagRequiredError, ErrorCode tagLimitExceededError) {
    long activityTagCount = countTagsByType(tags, TagType.ACTIVITY);
    if (activityTagCount == 0) {
      throw new BaseException(activityTagRequiredError);
    }
    if (activityTagCount > MAX_ACTIVITY_TAG_COUNT
        || countTagsByType(tags, TagType.INTEREST) > MAX_INTEREST_TAG_COUNT
        || countTagsByType(tags, TagType.TRAVEL_STYLE) > MAX_TRAVEL_STYLE_TAG_COUNT) {
      throw new BaseException(tagLimitExceededError);
    }
  }

  private static long countTagsByType(List<Tag> tags, TagType tagType) {
    return tags.stream()
        .filter(tag -> tag.getTagType() == tagType)
        .count();
  }
}
