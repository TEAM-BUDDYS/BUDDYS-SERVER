package org.sopt.buddys.domain.place.service.result;

import java.time.LocalDateTime;
import org.sopt.buddys.domain.place.entity.PlaceCategory;

public record BookmarkedPlaceResult(
    String placeId,
    String name,
    PlaceCategory category,
    String address,
    Double latitude,
    Double longitude,
    String photoUrl,
    String googleMapsUrl,
    LocalDateTime bookmarkedAt
) {
}
