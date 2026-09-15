package org.sopt.buddys.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PlaceBookmarkResponse(
    @Schema(description = "요청 처리 후 로그인 유저의 저장 여부", example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED)
    boolean bookmarked
) {
  public static PlaceBookmarkResponse of(boolean bookmarked) {
    return new PlaceBookmarkResponse(bookmarked);
  }
}
