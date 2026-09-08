package org.sopt.buddys.domain.magazine.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.LocalDate;
import java.util.List;
import org.sopt.buddys.domain.magazine.entity.Magazine;
import org.sopt.buddys.domain.magazine.service.result.MagazineListResult;
import org.sopt.buddys.domain.magazine.service.result.MagazineListResult.MagazineSummaryResult;

public record MagazineListResponse(
    @Schema(description = "조회 연도", example = "2026", requiredMode = RequiredMode.REQUIRED)
    int year,

    @Schema(description = "조회 월", example = "8", requiredMode = RequiredMode.REQUIRED)
    int month,

    @Schema(description = "조회 조건에 해당하는 전체 매거진 수", example = "2", requiredMode = RequiredMode.REQUIRED)
    long totalCount,

    @Schema(description = "현재 페이지 번호. 0부터 시작합니다.", example = "0", requiredMode = RequiredMode.REQUIRED)
    int page,

    @Schema(description = "페이지 크기", example = "10", requiredMode = RequiredMode.REQUIRED)
    int size,

    @Schema(description = "다음 페이지 존재 여부", example = "false", requiredMode = RequiredMode.REQUIRED)
    boolean hasNext,

    @Schema(description = "매거진 목록", requiredMode = RequiredMode.REQUIRED)
    List<MagazineSummaryResponse> magazines
) {

  public MagazineListResponse {
    magazines = List.copyOf(magazines);
  }

  public static MagazineListResponse from(MagazineListResult result) {
    return new MagazineListResponse(
        result.year(),
        result.month(),
        result.totalCount(),
        result.page(),
        result.size(),
        result.hasNext(),
        result.magazines().stream()
            .map(MagazineSummaryResponse::from)
            .toList()
    );
  }

  public record MagazineSummaryResponse(
      @Schema(description = "매거진 ID", example = "1", requiredMode = RequiredMode.REQUIRED)
      Long magazineId,

      @Schema(description = "매거진 제목", example = "유럽 교환학생이라면 루프트한자 학생 혜택부터!",
          requiredMode = RequiredMode.REQUIRED)
      String title,

      @Schema(description = "매거진 목록용 요약 문구",
          example = "유럽 교환학생을 준비하고 있다면 꼭 확인해야 할 혜택을 소개해요.", requiredMode = RequiredMode.REQUIRED)
      String summary,

      @Schema(description = "썸네일 이미지 URL", example = "https://example.com/magazines/1.png",
          requiredMode = RequiredMode.REQUIRED)
      String thumbnailImageUrl,

      @Schema(description = "발행일", example = "2026-08-20", requiredMode = RequiredMode.REQUIRED)
      LocalDate publishedAt,

      @Schema(description = "인스타그램 게시물 링크", example = "https://www.instagram.com/p/ABC123/",
          requiredMode = RequiredMode.REQUIRED)
      String externalUrl,

      @Schema(description = "로그인한 사용자의 저장 여부", example = "true", requiredMode = RequiredMode.REQUIRED)
      boolean isBookmarked
  ) {

    private static MagazineSummaryResponse from(MagazineSummaryResult result) {
      Magazine magazine = result.magazine();
      return new MagazineSummaryResponse(
          magazine.getId(),
          magazine.getTitle(),
          magazine.getSummary(),
          magazine.getThumbnailImageUrl(),
          magazine.getPublishedAt(),
          magazine.getExternalUrl(),
          result.isBookmarked()
      );
    }
  }
}
