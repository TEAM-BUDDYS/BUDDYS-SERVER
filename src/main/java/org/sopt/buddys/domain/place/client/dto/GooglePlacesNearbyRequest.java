package org.sopt.buddys.domain.place.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GooglePlacesNearbyRequest(
    List<String> includedTypes,
    int maxResultCount,
    GoogleLocationRestriction locationRestriction,
    String languageCode
) {
}
