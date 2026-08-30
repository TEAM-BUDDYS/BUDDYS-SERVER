package org.sopt.buddys.domain.place.service.result;

import java.time.LocalDateTime;
import java.util.List;
import org.sopt.buddys.domain.place.entity.PlaceCategory;

public record BookmarkedPlaceListResult(
    List<BookmarkedPlaceResult> places,
    int page,
    int size,
    boolean hasNext
) {

  public BookmarkedPlaceListResult {
    places = List.copyOf(places);
  }

  public record BookmarkedPlaceResult(
      String placeId,
      String name,
      PlaceCategory category,
      String address,
      Double latitude,
      Double longitude,
      String photoUrl,
      LocalDateTime bookmarkedAt
  ) {
  }
}
