package org.sopt.buddys.domain.post.service.result;

import java.util.List;
import org.sopt.buddys.domain.post.entity.AgeCondition;
import org.sopt.buddys.domain.post.entity.Post;

public record PostDetailResult(
    Post post,
    List<String> imageUrls,
    List<AgeCondition> ageConditions,
    List<TagResult> tags
) {

  public PostDetailResult {
    imageUrls = List.copyOf(imageUrls);
    ageConditions = List.copyOf(ageConditions);
    tags = List.copyOf(tags);
  }

  public record TagResult(
      Long tagId,
      String name
  ) {
  }
}
