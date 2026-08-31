package org.sopt.buddys.domain.comment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import org.sopt.buddys.domain.comment.service.result.CourseCommentListResult;

public record CourseCommentListResponse(
    @Schema(description = "댓글 목록")
    List<CourseCommentResponse> comments,

    @Schema(description = "현재 페이지 번호. 0부터 시작합니다.", example = "0")
    int page,

    @Schema(description = "페이지 크기", example = "20")
    int size,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    boolean hasNext
) {

  public CourseCommentListResponse {
    comments = List.copyOf(comments);
  }

  public static CourseCommentListResponse from(CourseCommentListResult result) {
    return new CourseCommentListResponse(
        result.comments()
            .stream()
            .map(CourseCommentResponse::from)
            .toList(),
        result.page(),
        result.size(),
        result.hasNext()
    );
  }

  public record CourseCommentResponse(
      @Schema(description = "댓글 ID", example = "1")
      Long commentId,

      @Schema(description = "댓글 작성자 사용자 ID", example = "10")
      Long writerId,

      @Schema(description = "댓글 작성자 이름", example = "유저 1")
      String writerName,

      @Schema(description = "댓글 작성자 프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
      String writerProfileImageUrl,

      @Schema(description = "댓글 내용", example = "저도 같이 가고 싶어요!")
      String content,

      @Schema(description = "댓글 작성 시간", example = "2026-07-09T21:00:00")
      LocalDateTime createdAt,

      @Schema(description = "상대 작성 시간", example = "1시간 전")
      String timeAgo
  ) {

    private static CourseCommentResponse from(CourseCommentListResult.CourseCommentResult result) {
      return new CourseCommentResponse(
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
