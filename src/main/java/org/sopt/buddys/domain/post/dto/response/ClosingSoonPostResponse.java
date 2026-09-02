package org.sopt.buddys.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.service.result.ClosingSoonPostResult;

public record ClosingSoonPostResponse(
    @Schema(description = "마감 임박 게시글 목록")
    List<ClosingSoonPostSummaryResponse> content
) {

  public ClosingSoonPostResponse {
    content = List.copyOf(content);
  }

  public static ClosingSoonPostResponse from(ClosingSoonPostResult result) {
    return new ClosingSoonPostResponse(
        result.content().stream()
            .map(ClosingSoonPostSummaryResponse::from)
            .toList()
    );
  }

  public record ClosingSoonPostSummaryResponse(
      @Schema(description = "게시글 ID", example = "1")
      Long postId,

      @Schema(description = "국가")
      ClosingSoonPostCountryResponse country,

      @Schema(description = "게시글 제목", example = "파리 9월 출국 프랑스 교환학생 동행 구함")
      String title,

      @Schema(description = "게시글 본문", example = "안녕하세요! 이번 가을 학기 교환학생으로 갑니다.")
      String content,

      @Schema(description = "동행 시작일", example = "2026-09-01")
      LocalDate startDate,

      @Schema(description = "동행 종료일", example = "2026-09-04")
      LocalDate endDate,

      @Schema(description = "동행 총 일수", example = "4")
      int durationDays,

      @Schema(description = "대표 이미지 URL", example = "https://example.com/posts/1.jpg", nullable = true)
      String thumbnailImageUrl,

      @Schema(description = "현재 사용자의 게시글 저장 여부", example = "true")
      boolean isSaved
  ) {

    private static ClosingSoonPostSummaryResponse from(
        ClosingSoonPostResult.ClosingSoonPostSummaryResult result
    ) {
      Post post = result.post();
      return new ClosingSoonPostSummaryResponse(
          post.getId(),
          ClosingSoonPostCountryResponse.from(post),
          post.getTitle(),
          post.getContent(),
          post.getStartDate(),
          post.getEndDate(),
          toDurationDays(post.getStartDate(), post.getEndDate()),
          result.thumbnailImageUrl(),
          result.saved()
      );
    }

    private static int toDurationDays(LocalDate startDate, LocalDate endDate) {
      return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
  }

  public record ClosingSoonPostCountryResponse(
      @Schema(description = "국가 ID", example = "1")
      Long countryId,

      @Schema(description = "국가 이름", example = "France")
      String name
  ) {

    private static ClosingSoonPostCountryResponse from(Post post) {
      return new ClosingSoonPostCountryResponse(post.getCountry().getId(), post.getCountry().getName());
    }
  }
}
