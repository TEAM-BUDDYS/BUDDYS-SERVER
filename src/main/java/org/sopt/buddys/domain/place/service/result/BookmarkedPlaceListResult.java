package org.sopt.buddys.domain.place.service.result;

import java.util.List;

public record BookmarkedPlaceListResult(
    List<BookmarkedPlaceResult> places,
    int page,
    int size,
    boolean hasNext
) {

  public BookmarkedPlaceListResult {
    places = List.copyOf(places);
  }
}
