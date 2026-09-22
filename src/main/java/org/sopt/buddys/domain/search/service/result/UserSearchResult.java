package org.sopt.buddys.domain.search.service.result;

import java.util.List;

public record UserSearchResult(
    List<UserSummaryResult> content,
    int page,
    int size,
    boolean hasNext
) {

  public UserSearchResult {
    content = List.copyOf(content);
  }

  public record UserSummaryResult(
      Long userId,
      String nickname,
      String profileImageUrl
  ) {
  }
}
