package org.sopt.buddys.domain.search.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SearchRequest(
    @Schema(description = "검색어", example = "파리", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    String keyword,

    @Schema(description = "페이지 번호. 0 이상입니다.", example = "0", defaultValue = "0")
    @Min(0)
    Integer page,

    @Schema(description = "페이지 크기. 1 이상 100 이하입니다.", example = "5", defaultValue = "5")
    @Min(1)
    @Max(100)
    Integer size
) {

  public String normalizedKeyword() {
    return keyword.trim();
  }

  public int pageOrDefault() {
    return page == null ? 0 : page;
  }

  public int sizeOrDefault() {
    return size == null ? 5 : size;
  }
}
