package org.sopt.buddys.domain.place.service.result;

import java.util.List;

public record BookmarkedPlaceMarkersResult(
    List<BookmarkedPlaceResult> places,
    boolean truncated
) {

  public BookmarkedPlaceMarkersResult {
    places = List.copyOf(places);
  }
}
