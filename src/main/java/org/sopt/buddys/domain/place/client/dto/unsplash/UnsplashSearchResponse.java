package org.sopt.buddys.domain.place.client.dto.unsplash;

import java.util.List;

public record UnsplashSearchResponse(
    List<UnsplashPhoto> results
) {

  public List<UnsplashPhoto> resultsOrEmpty() {
    return results == null ? List.of() : results;
  }
}
