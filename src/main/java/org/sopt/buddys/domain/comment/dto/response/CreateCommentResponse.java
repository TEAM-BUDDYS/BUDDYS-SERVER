package org.sopt.buddys.domain.comment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.comment.entity.Comment;

public record CreateCommentResponse(
    @Schema(description = "생성된 댓글 ID", example = "1")
    Long commentId
) {

  public static CreateCommentResponse from(Comment comment) {
    return new CreateCommentResponse(comment.getId());
  }
}
