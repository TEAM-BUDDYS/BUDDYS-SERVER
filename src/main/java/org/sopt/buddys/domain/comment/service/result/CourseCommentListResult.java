package org.sopt.buddys.domain.comment.service.result;

import java.time.LocalDateTime;
import java.util.List;

public record CourseCommentListResult(
    List<CourseCommentResult> comments,
    int page,
    int size,
    boolean hasNext
) {

  public CourseCommentListResult {
    comments = List.copyOf(comments);
  }

  public record CourseCommentResult(
      Long commentId,
      Long writerId,
      String writerName,
      String writerProfileImageUrl,
      String content,
      LocalDateTime createdAt,
      String timeAgo
  ) {
  }
}
