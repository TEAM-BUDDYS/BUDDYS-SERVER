package org.sopt.buddys.domain.magazine.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.sopt.buddys.domain.magazine.entity.MagazineCategory;
import org.sopt.buddys.domain.magazine.entity.MagazineSort;

public record MagazineListRequest(
    @Schema(
        description = "매거진 카테고리(SUPPORT: 지원, DEPARTURE_PREP: 출국 준비, "
            + "LOCAL_SETTLEMENT: 현지 정착, TRAVEL: 여행)",
        example = "SUPPORT",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    MagazineCategory category,

    @Schema(
        description = "제목 또는 요약에서 부분 일치로 검색할 검색어. "
            + "생략하거나 공백만 입력하면 검색 조건을 적용하지 않습니다.",
        example = "교환학생"
    )
    String keyword,

    @Schema(description = "정렬 기준(LATEST: 최신순, BOOKMARK: 전체 저장 수 순)",
        example = "LATEST", defaultValue = "LATEST")
    MagazineSort sort,

    @Schema(description = "페이지 번호. 0 이상입니다.", example = "0", defaultValue = "0")
    @Min(0)
    Integer page,

    @Schema(description = "페이지 크기. 1 이상 100 이하입니다.", example = "10", defaultValue = "10")
    @Min(1)
    @Max(100)
    Integer size
) {

  public String normalizedKeyword() {
    if (keyword == null) {
      return null;
    }
    String normalizedKeyword = keyword.strip();
    return normalizedKeyword.isEmpty() ? null : normalizedKeyword;
  }

  public MagazineSort sortOrDefault() {
    return sort == null ? MagazineSort.LATEST : sort;
  }

  public int pageOrDefault() {
    return page == null ? 0 : page;
  }

  public int sizeOrDefault() {
    return size == null ? 10 : size;
  }
}
