package org.sopt.buddys.domain.search.service.result;

import java.util.List;

public record SearchSuggestionResult(
    List<SuggestionResult> suggestions
) {

  public SearchSuggestionResult {
    suggestions = List.copyOf(suggestions);
  }

  public record SuggestionResult(
      SearchSuggestionType type,
      String keyword
  ) {
  }
}
