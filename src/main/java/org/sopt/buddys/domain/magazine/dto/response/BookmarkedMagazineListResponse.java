package org.sopt.buddys.domain.magazine.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.LocalDate;
import java.util.List;
import org.sopt.buddys.domain.magazine.entity.Magazine;
import org.sopt.buddys.domain.magazine.service.result.BookmarkedMagazineListResult;

public record BookmarkedMagazineListResponse(
    @Schema(description = "저장한 매거진 목록", requiredMode = RequiredMode.REQUIRED)
    List<BookmarkedMagazineResponse> magazines,

    @Schema(description = "현재 페이지 번호. 0부터 시작합니다.", example = "0", requiredMode = RequiredMode.REQUIRED)
    int page,

    @Schema(description = "페이지 크기", example = "20", requiredMode = RequiredMode.REQUIRED)
    int size,

    @Schema(description = "다음 페이지 존재 여부", example = "false", requiredMode = RequiredMode.REQUIRED)
    boolean hasNext
) {

  public BookmarkedMagazineListResponse {
    magazines = List.copyOf(magazines);
  }

  public static BookmarkedMagazineListResponse from(BookmarkedMagazineListResult result) {
    return new BookmarkedMagazineListResponse(
        result.magazines().stream().map(BookmarkedMagazineResponse::from).toList(),
        result.page(),
        result.size(),
        result.hasNext()
    );
  }

  public record BookmarkedMagazineResponse(
      @Schema(description = "매거진 ID", example = "1", requiredMode = RequiredMode.REQUIRED)
      Long magazineId,

      @Schema(description = "매거진 제목", example = "교환학생을 위한 항공권 예약 팁", requiredMode = RequiredMode.REQUIRED)
      String title,

      @Schema(description = "매거진 목록용 요약 문구", example = "항공권을 저렴하게 예약하는 방법을 소개해요.",
          requiredMode = RequiredMode.REQUIRED)
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

    private static BookmarkedMagazineResponse from(Magazine magazine) {
      return new BookmarkedMagazineResponse(
          magazine.getId(),
          magazine.getTitle(),
          magazine.getSummary(),
          magazine.getThumbnailImageUrl(),
          magazine.getPublishedAt(),
          magazine.getExternalUrl(),
          true
      );
    }
  }
}
