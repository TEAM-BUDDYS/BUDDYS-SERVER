package org.sopt.buddys.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.place.service.result.PlaceSearchResult;

public record PlaceSearchResponse(
    List<PlaceResponse> places,
    @Schema(description = "다음 페이지 조회용 토큰. 다음 페이지 없으면 null", nullable = true)
    String nextPageToken
) {
  public static PlaceSearchResponse from(PlaceSearchResult result) {
    return new PlaceSearchResponse(
        result.places().stream().map(PlaceResponse::from).toList(),
        result.nextPageToken()
    );
  }
}