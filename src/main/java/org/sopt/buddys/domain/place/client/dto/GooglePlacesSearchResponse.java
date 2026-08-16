package org.sopt.buddys.domain.place.client.dto;

import java.util.List;

public record GooglePlacesSearchResponse(
    List<GooglePlace> places,
    String nextPageToken
) {
  public List<GooglePlace> placesOrEmpty() {
    return places == null ? List.of() : places;
  }
}
