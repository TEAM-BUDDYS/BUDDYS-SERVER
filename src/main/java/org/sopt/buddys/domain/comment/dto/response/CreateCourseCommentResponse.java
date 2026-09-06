package org.sopt.buddys.domain.comment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.comment.entity.CourseComment;

public record CreateCourseCommentResponse(
    @Schema(description = "생성된 댓글 ID", example = "1")
    Long commentId
) {

  public static CreateCourseCommentResponse from(CourseComment comment) {
    return new CreateCourseCommentResponse(comment.getId());
  }
}
