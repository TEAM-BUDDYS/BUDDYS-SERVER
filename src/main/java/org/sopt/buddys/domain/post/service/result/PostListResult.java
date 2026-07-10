package org.sopt.buddys.domain.post.service.result;

import java.util.List;
import org.sopt.buddys.domain.post.entity.Post;

public record PostListResult(
    List<PostSummaryResult> content,
    int page,
    int size,
    boolean hasNext
) {

  public PostListResult {
    content = List.copyOf(content);
  }

  public record PostSummaryResult(
      Post post,
      String thumbnailImageUrl
  ) {
  }
}
