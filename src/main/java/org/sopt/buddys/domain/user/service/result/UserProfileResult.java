package org.sopt.buddys.domain.user.service.result;

import java.util.List;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.User;

public record UserProfileResult(
    User user,
    List<String> representativeTags,
    List<TagGroupResult> allTags
) {

  public UserProfileResult {
    representativeTags = List.copyOf(representativeTags);
    allTags = List.copyOf(allTags);
  }

  public record TagGroupResult(
      TagType tagType,
      List<String> tags
  ) {

    public TagGroupResult {
      tags = List.copyOf(tags);
    }
  }
}
