package org.sopt.buddys.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.place.service.result.BookmarkedPlaceListResult;

public record BookmarkedPlaceListResponse(
    @Schema(description = "저장한 장소 목록. 최근 저장순", requiredMode = Schema.RequiredMode.REQUIRED)
    List<BookmarkedPlaceResponse> places,

    @Schema(description = "현재 페이지 번호. 0부터 시작합니다.", example = "0",
        requiredMode = Schema.RequiredMode.REQUIRED)
    int page,

    @Schema(description = "페이지 크기", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    int size,

    @Schema(description = "다음 페이지 존재 여부", example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED)
    boolean hasNext
) {

  public BookmarkedPlaceListResponse {
    places = List.copyOf(places);
  }

  public static BookmarkedPlaceListResponse from(BookmarkedPlaceListResult result) {
    return new BookmarkedPlaceListResponse(
        result.places().stream()
            .map(BookmarkedPlaceResponse::from)
            .toList(),
        result.page(),
        result.size(),
        result.hasNext()
    );
  }
}
