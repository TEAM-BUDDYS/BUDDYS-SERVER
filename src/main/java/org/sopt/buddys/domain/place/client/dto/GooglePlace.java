package org.sopt.buddys.domain.place.client.dto;

import java.util.List;

public record GooglePlace(
    String id,
    GoogleDisplayName displayName,
    String primaryType,
    String formattedAddress,
    GoogleLatLng location,
    List<GooglePhoto> photos
) {
}