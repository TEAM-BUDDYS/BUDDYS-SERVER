package org.sopt.buddys.domain.place.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GooglePlacesSearchRequest(
    String textQuery,
    GoogleLocationBias locationBias,
    String pageToken,
    String languageCode
) {
}
