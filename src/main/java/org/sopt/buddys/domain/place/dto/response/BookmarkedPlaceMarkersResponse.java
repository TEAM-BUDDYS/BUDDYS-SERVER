package org.sopt.buddys.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.place.service.result.BookmarkedPlaceMarkersResult;

public record BookmarkedPlaceMarkersResponse(
    @Schema(description = "요청한 지도 영역 안에 있는, 로그인 유저가 저장한 장소 목록",
        requiredMode = Schema.RequiredMode.REQUIRED)
    List<BookmarkedPlaceResponse> places,

    @Schema(description = "영역 안 저장 장소가 상한을 초과해 일부만 반환됐는지 여부. true면 지도를 확대해 다시 조회하도록 안내",
        example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean truncated
) {

  public BookmarkedPlaceMarkersResponse {
    places = List.copyOf(places);
  }

  public static BookmarkedPlaceMarkersResponse from(BookmarkedPlaceMarkersResult result) {
    return new BookmarkedPlaceMarkersResponse(
        result.places().stream()
            .map(BookmarkedPlaceResponse::from)
            .toList(),
        result.truncated()
    );
  }
}
