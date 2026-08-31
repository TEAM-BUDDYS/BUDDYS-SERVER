package org.sopt.buddys.domain.tag.service.result;

import java.util.List;
import org.sopt.buddys.domain.tag.entity.TagType;

public record TagGroupResult(
    TagType tagType,
    List<TagResult> tags
) {

  public TagGroupResult {
    tags = List.copyOf(tags);
  }

  public record TagResult(
      Long id,
      String name
  ) {
  }
}
