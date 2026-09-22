package org.sopt.buddys.domain.post.service.result;

import java.util.List;
import org.sopt.buddys.domain.post.entity.Post;

public record ClosingSoonPostResult(
    List<ClosingSoonPostSummaryResult> content
) {

  public ClosingSoonPostResult {
    content = List.copyOf(content);
  }

  public record ClosingSoonPostSummaryResult(
      Post post,
      String thumbnailImageUrl,
      boolean saved
  ) {
  }
}
