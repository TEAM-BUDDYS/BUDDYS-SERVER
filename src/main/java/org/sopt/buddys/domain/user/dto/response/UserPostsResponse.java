package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
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

      @Schema(description = "동행 시작일", example = "2026-07-23")
      LocalDate startDate,

      @Schema(description = "동행 종료일", example = "2026-07-28")
      LocalDate endDate
  ) {

    private static PostResponse from(UserPostsResult.PostResult result) {
      Post post = result.post();
      return new PostResponse(
          post.getId(),
          post.getTitle(),
          post.getContent(),
          result.thumbnailImageUrl(),
          post.getStartDate(),
          post.getEndDate()
      );
    }
  }
}
