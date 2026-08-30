package org.sopt.buddys.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import org.sopt.buddys.domain.place.entity.PlaceCategory;
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

  public record BookmarkedPlaceResponse(
      @Schema(description = "구글 place_id. 저장 취소 시 이 값을 사용",
          example = "ChIJN1t_tDeuEmsRUsoyG83frY4", requiredMode = Schema.RequiredMode.REQUIRED)
      String placeId,

      @Schema(description = "장소명 (저장 시점 스냅샷)", example = "루브르 박물관",
          requiredMode = Schema.RequiredMode.REQUIRED)
      String name,

      @Schema(description = "장소 카테고리", example = "TOURISM",
          requiredMode = Schema.RequiredMode.REQUIRED)
      PlaceCategory category,

      @Schema(description = "주소 (저장 시점 스냅샷). 구글이 주소를 안 줬으면 null",
          example = "Rue de Rivoli, 75001 Paris",
          requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
      String address,

      @Schema(description = "위도. 좌표를 저장하지 못했으면 null", example = "48.8606",
          requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
      Double latitude,

      @Schema(description = "경도. 좌표를 저장하지 못했으면 null", example = "2.3376",
          requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
      Double longitude,

      @Schema(description = "대표 사진 프록시 URL. 사진이 없으면 이 URL 호출 시 404가 반환됩니다.",
          example = "/api/v1/places/ChIJN1t_tDeuEmsRUsoyG83frY4/photo?maxWidth=400",
          requiredMode = Schema.RequiredMode.REQUIRED)
      String photoUrl,

      @Schema(description = "구글맵에서 이 장소를 여는 딥링크",
          example = "https://www.google.com/maps/search/?api=1&query=%EB%A3%A8%EB%B8%8C%EB%A5%B4+%EB%B0%95%EB%AC%BC%EA%B4%80&query_place_id=ChIJN1t_tDeuEmsRUsoyG83frY4",
          requiredMode = Schema.RequiredMode.REQUIRED)
      String googleMapsUrl,

      @Schema(description = "저장한 시각", example = "2026-08-30T21:00:00",
          requiredMode = Schema.RequiredMode.REQUIRED)
      LocalDateTime bookmarkedAt
  ) {

    private static BookmarkedPlaceResponse from(BookmarkedPlaceListResult.BookmarkedPlaceResult result) {
      return new BookmarkedPlaceResponse(
          result.placeId(),
          result.name(),
          result.category(),
          result.address(),
          result.latitude(),
          result.longitude(),
          result.photoUrl(),
          result.googleMapsUrl(),
          result.bookmarkedAt()
      );
    }
  }
}
