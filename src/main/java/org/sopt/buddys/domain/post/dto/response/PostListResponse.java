package org.sopt.buddys.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.service.result.PostListResult;

public record PostListResponse(
    @Schema(description = "게시글 목록")
    List<PostSummaryResponse> content,

    @Schema(description = "현재 페이지 번호. 0부터 시작합니다.", example = "0")
    int page,

    @Schema(description = "페이지 크기", example = "10")
    int size,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    boolean hasNext
) {

  public PostListResponse {
    content = List.copyOf(content);
  }

  public static PostListResponse from(PostListResult result) {
    return new PostListResponse(
        result.content().stream()
            .map(PostSummaryResponse::from)
            .toList(),
        result.page(),
        result.size(),
        result.hasNext()
    );
  }

  public record PostSummaryResponse(
      @Schema(description = "게시글 ID", example = "1")
      Long postId,

      @Schema(description = "게시글 제목", example = "주말에 파리 근교 함께 가실 분!")
      String title,

      @Schema(description = "게시글 본문", example = "같이 맛집이랑 관광지 다니실 분 구해요.")
      String content,

      @Schema(description = "국가")
      PostSummaryCountryResponse country,

      @Schema(description = "동행 시작일", example = "2026-07-23")
      LocalDate startDate,

      @Schema(description = "동행 종료일", example = "2026-07-28")
      LocalDate endDate,

      @Schema(description = "동행 총 일수", example = "6")
      int durationDays,

      @Schema(description = "모집 상태", example = "RECRUITING")
      PostStatus recruitmentStatus,

      @Schema(description = "대표 이미지 URL", example = "https://example.com/thumbnail.png")
      String thumbnailImageUrl
  ) {

    private static PostSummaryResponse from(PostListResult.PostSummaryResult result) {
      Post post = result.post();
      return new PostSummaryResponse(
          post.getId(),
          post.getTitle(),
          post.getContent(),
          PostSummaryCountryResponse.from(post),
          post.getStartDate(),
          post.getEndDate(),
          toDurationDays(post.getStartDate(), post.getEndDate()),
          post.getStatus(),
          result.thumbnailImageUrl()
      );
    }

    private static int toDurationDays(LocalDate startDate, LocalDate endDate) {
      return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
  }

  public record PostSummaryCountryResponse(
      @Schema(description = "국가 ID", example = "1")
      Long countryId,

      @Schema(description = "국가 이름", example = "France")
      String name
  ) {

    private static PostSummaryCountryResponse from(Post post) {
      return new PostSummaryCountryResponse(post.getCountry().getId(), post.getCountry().getName());
    }
  }
}
