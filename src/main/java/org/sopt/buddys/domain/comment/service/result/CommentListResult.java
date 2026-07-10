package org.sopt.buddys.domain.comment.service.result;

import java.time.LocalDateTime;
import java.util.List;

public record CommentListResult(
    List<CommentResult> comments,
    int page,
    int size,
    boolean hasNext
) {

  public CommentListResult {
    comments = List.copyOf(comments);
  }

  public record CommentResult(
      Long commentId,
      String writerName,
      String content,
      LocalDateTime createdAt,
      String timeAgo
  ) {
  }
}
