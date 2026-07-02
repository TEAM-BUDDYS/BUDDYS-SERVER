package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.user.service.result.UserPostsResult;

public record UserPostsResponse(
    @Schema(description = "사용자가 작성한 게시글 목록")
    List<PostResponse> posts,

    @Schema(description = "현재 페이지 번호. 0부터 시작합니다.", example = "0")
    int page,

    @Schema(description = "페이지 크기", example = "10")
    int size,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    boolean hasNext
) {

  public UserPostsResponse {
    posts = List.copyOf(posts);
  }

  public static UserPostsResponse from(UserPostsResult result) {
    return new UserPostsResponse(
        result.posts()
            .stream()
            .map(PostResponse::from)
            .toList(),
        result.page(),
        result.size(),
        result.hasNext()
    );
  }

  public record PostResponse(
      @Schema(description = "게시글 ID", example = "1")
      Long postId,

      @Schema(description = "게시글 제목", example = "주말에 파리 근교 함께 가실 분!")
      String title,

      @Schema(description = "게시글 본문", example = "안녕하세요. 스쿠버다이빙 가실 분 구합니다.")
      String content,

      @Schema(description = "게시글 썸네일 이미지 URL", example = "https://example.com/post-thumbnail.png")
      String thumbnailImageUrl,

      @Schema(description = "게시글 일정 표시 텍스트", example = "26.09.06 ~ 26.09.19 (13일)")
      String dateText
  ) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yy.MM.dd");

    private static PostResponse from(UserPostsResult.PostResult result) {
      Post post = result.post();
      return new PostResponse(
          post.getId(),
          post.getTitle(),
          post.getContent(),
          result.thumbnailImageUrl(),
          toDateText(post.getStartDate(), post.getEndDate())
      );
    }

    private static String toDateText(LocalDate startDate, LocalDate endDate) {
      if (startDate == null || endDate == null) {
        return null;
      }
      long days = ChronoUnit.DAYS.between(startDate, endDate);
      return "%s ~ %s (%d일)".formatted(
          startDate.format(DATE_FORMATTER),
          endDate.format(DATE_FORMATTER),
          days
      );
    }
  }
}
