package org.sopt.buddys.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.place.entity.PlaceCategory;
import org.sopt.buddys.domain.place.service.result.PlaceSearchResult.PlaceSearchItemResult;

public record PlaceResponse(
    @Schema(description = "구글 place_id. 저장(북마크) 시 이 값을 사용", example = "ChIJN1t_tDeuEmsRUsoyG83frY4", requiredMode = Schema.RequiredMode.REQUIRED)
    String placeId,
    @Schema(description = "장소명", example = "루브르 박물관", requiredMode = Schema.RequiredMode.REQUIRED) String name,
    @Schema(description = "장소 카테고리", example = "TOURISM") PlaceCategory category,
    @Schema(description = "주소", example = "Rue de Rivoli, 75001 Paris", requiredMode = Schema.RequiredMode.REQUIRED) String address,
    @Schema(description = "위도", example = "48.8606", requiredMode = Schema.RequiredMode.REQUIRED) Double latitude,
    @Schema(description = "경도", example = "2.3376", requiredMode = Schema.RequiredMode.REQUIRED) Double longitude,
    @Schema(description = "로그인 유저의 저장 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED) boolean bookmarked,
    @Schema(description = "대표 사진 URL", nullable = true) String photoUrl
) {
  public static PlaceResponse from(PlaceSearchItemResult result) {
    return new PlaceResponse(
        result.placeId(),
        result.name(),
        result.category(),
        result.address(),
        result.latitude(),
        result.longitude(),
        result.bookmarked(),
        result.photoUrl()
    );
  }
}