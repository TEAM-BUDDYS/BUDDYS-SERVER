package org.sopt.buddys.domain.user.service.result;

import java.util.List;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.User;

public record UserProfileResult(
    User user,
    List<OrderedTagResult> orderedTags
) {

  public UserProfileResult {
    orderedTags = List.copyOf(orderedTags);
  }

  public record OrderedTagResult(
      Long id,
      String name,
      TagType tagType,
      int displayOrder
  ) {}
}
