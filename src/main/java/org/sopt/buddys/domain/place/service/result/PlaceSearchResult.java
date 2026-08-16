package org.sopt.buddys.domain.place.service.result;

import java.util.List;
import org.sopt.buddys.domain.place.entity.PlaceCategory;

public record PlaceSearchResult(
    List<PlaceSearchItemResult> places,
    String nextPageToken
) {

  public PlaceSearchResult {
    places = List.copyOf(places);
  }

  public record PlaceSearchItemResult(
      String placeId,
      String name,
      PlaceCategory category,
      String address,
      Double latitude,
      Double longitude,
      boolean bookmarked,
      String photoUrl
  ) {
  }
}
