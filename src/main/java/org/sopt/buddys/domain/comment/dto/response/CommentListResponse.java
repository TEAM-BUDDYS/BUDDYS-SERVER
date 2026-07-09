package org.sopt.buddys.domain.comment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record CommentListResponse(
    @Schema(description = "댓글 목록")
    List<CommentResponse> comments
) {

  public CommentListResponse {
    comments = List.copyOf(comments);
  }

  public static CommentListResponse of(List<CommentResponse> comments) {
    return new CommentListResponse(comments);
  }

  public record CommentResponse(
      @Schema(description = "댓글 ID", example = "1")
      Long commentId,

      @Schema(description = "댓글 작성자 이름", example = "유저 1")
      String writerName,

      @Schema(description = "댓글 내용", example = "저도 관심 있어요! DM 보낼게요.")
      String content,

      @Schema(description = "댓글 작성 시간", example = "2026-07-09T21:00:00")
      LocalDateTime createdAt,

      @Schema(description = "상대 작성 시간", example = "1시간 전")
      String timeAgo
  ) {
  }
}
