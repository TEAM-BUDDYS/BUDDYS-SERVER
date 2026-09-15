package org.sopt.buddys.domain.magazine.service.result;

import java.util.List;
import org.sopt.buddys.domain.magazine.entity.Magazine;

public record MagazineListResult(
    int year,
    int month,
    long totalCount,
    int page,
    int size,
    boolean hasNext,
    List<MagazineSummaryResult> magazines
) {

  public MagazineListResult {
    magazines = List.copyOf(magazines);
  }

  public record MagazineSummaryResult(
      Magazine magazine,
      boolean isBookmarked
  ) {
  }
}
