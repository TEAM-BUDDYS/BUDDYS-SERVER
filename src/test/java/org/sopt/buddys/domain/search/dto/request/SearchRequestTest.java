package org.sopt.buddys.domain.search.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.search.service.command.SearchSort;

class SearchRequestTest {

  @Test
  void sort_defaultsToLatestAndPreservesExplicitValues() {
    assertThat(new SearchRequest(" Paris ", null, null, null, null).sortOrDefault())
        .isEqualTo(SearchSort.LATEST);
    for (SearchSort sort : SearchSort.values()) {
      var request = new SearchRequest(" Paris ", 2, 10, "POST", sort);
      assertThat(request.sortOrDefault()).isEqualTo(sort);
      assertThat(request.normalizedKeyword()).isEqualTo("Paris");
      assertThat(request.pageOrDefault()).isEqualTo(2);
      assertThat(request.sizeOrDefault()).isEqualTo(10);
    }
  }
}
