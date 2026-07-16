package org.sopt.buddys.domain.comment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import org.sopt.buddys.domain.comment.service.result.CommentListResult;

public record CommentListResponse(
    @Schema(description = "댓글 목록")
    List<CommentResponse> comments,

    @Schema(description = "현재 페이지 번호. 0부터 시작합니다.", example = "0")
    int page,

    @Schema(description = "페이지 크기", example = "20")
    int size,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    boolean hasNext
) {

  public CommentListResponse {
    comments = List.copyOf(comments);
  }

  public static CommentListResponse of(
      List<CommentResponse> comments,
      int page,
      int size,
      boolean hasNext
  ) {
    return new CommentListResponse(comments, page, size, hasNext);
  }

  public static CommentListResponse from(CommentListResult result) {
    return new CommentListResponse(
        result.comments()
            .stream()
            .map(CommentResponse::from)
            .toList(),
        result.page(),
        result.size(),
        result.hasNext()
    );
  }

  public record CommentResponse(
      @Schema(description = "댓글 ID", example = "1")
      Long commentId,

      @Schema(description = "댓글 작성자 사용자 ID", example = "10")
      Long writerId,

      @Schema(description = "댓글 작성자 이름", example = "유저 1")
      String writerName,

      @Schema(description = "댓글 작성자 프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
      String writerProfileImageUrl,

      @Schema(description = "댓글 내용", example = "저도 관심 있어요! DM 보낼게요.")
      String content,

      @Schema(description = "댓글 작성 시간", example = "2026-07-09T21:00:00")
      LocalDateTime createdAt,

      @Schema(description = "상대 작성 시간", example = "1시간 전")
      String timeAgo
  ) {

    private static CommentResponse from(CommentListResult.CommentResult result) {
      return new CommentResponse(
          result.commentId(),
          result.writerId(),
          result.writerName(),
          result.writerProfileImageUrl(),
          result.content(),
          result.createdAt(),
          result.timeAgo()
      );
    }
  }
}
