package org.sopt.buddys.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.recommendation.service.result.RecommendedPostResult;

public record RecommendedPostResponse(
    @Schema(description = "게시글 ID", example = "1") Long postId,
    @Schema(description = "게시글 제목", example = "같이 유럽 여행 가실 분!") String title,
    @Schema(description = "게시글 내용") String content,
    @Schema(description = "모집 기간") PeriodResponse period,
    @Schema(description = "썸네일 이미지 주소", nullable = true) String thumbnailUrl,
    @Schema(description = "작성자 프로필 이미지 URL", nullable = true) String authorProfileImageUrl,
    @Schema(description = "게시글 국가") CountryResponse country,
    @Schema(description = "조회수", example = "10") Long viewCount
) {
  public static RecommendedPostResponse from(RecommendedPostResult result) {
    Post post = result.post();
    return new RecommendedPostResponse(
        post.getId(),
        post.getTitle(),
        post.getContent(),
        new PeriodResponse(post.getStartDate(), post.getEndDate()),
        result.thumbnailUrl(),
        post.getAuthor().getProfileImageUrl(),
        CountryResponse.from(post),
        post.getViewCount()
    );
  }

  public record PeriodResponse(
      @Schema(description = "시작일", example = "2027-03-01") LocalDate startDate,
      @Schema(description = "종료일", example = "2027-08-31") LocalDate endDate
  ) {}

  public record CountryResponse(
      @Schema(description = "국가 ID", example = "1") Long countryId,
      @Schema(description = "국가 이름", example = "France") String name
  ) {

    private static CountryResponse from(Post post) {
      return new CountryResponse(post.getCountry().getId(), post.getCountry().getName());
    }
  }
}
