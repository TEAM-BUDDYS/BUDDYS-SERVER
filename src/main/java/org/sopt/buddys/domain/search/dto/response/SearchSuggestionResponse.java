package org.sopt.buddys.domain.search.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.search.service.result.SearchSuggestionResult;
import org.sopt.buddys.domain.search.service.result.SearchSuggestionType;

public record SearchSuggestionResponse(
    @Schema(description = "자동완성 검색어 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    List<SuggestionResponse> suggestions
) {

  public SearchSuggestionResponse {
    suggestions = List.copyOf(suggestions);
  }

  public static SearchSuggestionResponse from(SearchSuggestionResult result) {
    return new SearchSuggestionResponse(
        result.suggestions().stream()
            .map(SuggestionResponse::from)
            .toList()
    );
  }

  public record SuggestionResponse(
      @Schema(description = "자동완성 타입", example = "CITY", requiredMode = Schema.RequiredMode.REQUIRED)
      SearchSuggestionType type,

      @Schema(description = "자동완성 검색어", example = "파리", requiredMode = Schema.RequiredMode.REQUIRED)
      String keyword
  ) {

    private static SuggestionResponse from(SearchSuggestionResult.SuggestionResult result) {
      return new SuggestionResponse(result.type(), result.keyword());
    }
  }
}
