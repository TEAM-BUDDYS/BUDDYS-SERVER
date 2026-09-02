package org.sopt.buddys.domain.place.client.dto;

import java.util.List;

public record GooglePlace(
    String id,
    GoogleDisplayName displayName,
    String primaryType,
    List<String> types,
    String formattedAddress,
    List<GoogleAddressComponent> addressComponents,
    GoogleLatLng location,
    List<GooglePhoto> photos
) {
}
