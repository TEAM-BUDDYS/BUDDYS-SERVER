package org.sopt.buddys.domain.search.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SearchSuggestionRequest(
    @Schema(description = "자동완성 검색어", example = "파", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    String keyword,

    @Schema(description = "자동완성 후보 개수. 1 이상 20 이하입니다.", example = "8", defaultValue = "8")
    @Min(1)
    @Max(20)
    Integer size
) {

  public String normalizedKeyword() {
    return keyword.trim();
  }

  public int sizeOrDefault() {
    return size == null ? 8 : size;
  }
}
