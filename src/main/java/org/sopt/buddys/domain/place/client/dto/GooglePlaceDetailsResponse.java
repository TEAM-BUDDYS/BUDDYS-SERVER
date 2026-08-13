package org.sopt.buddys.domain.place.client.dto;

import java.util.List;

public record GooglePlaceDetailsResponse(
    List<GooglePhoto> photos
) {
  public List<GooglePhoto> photosOrEmpty() {
    return photos == null ? List.of() : photos;
  }
}