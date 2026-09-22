package org.sopt.buddys.domain.magazine.service.result;

import java.util.List;
import org.sopt.buddys.domain.magazine.entity.Magazine;

public record BookmarkedMagazineListResult(
    List<Magazine> magazines,
    int page,
    int size,
    boolean hasNext
) {

  public BookmarkedMagazineListResult {
    magazines = List.copyOf(magazines);
  }
}
