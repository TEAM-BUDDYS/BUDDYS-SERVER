package org.sopt.buddys.domain.magazine.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import org.sopt.buddys.domain.magazine.service.result.MagazineBookmarkResult;

public record MagazineBookmarkResponse(
    @Schema(description = "매거진 ID", example = "1", requiredMode = RequiredMode.REQUIRED)
    Long magazineId,

    @Schema(description = "매거진 저장 여부", example = "true", requiredMode = RequiredMode.REQUIRED)
    boolean isBookmarked
) {

  public static MagazineBookmarkResponse from(MagazineBookmarkResult result) {
    return new MagazineBookmarkResponse(result.magazineId(), result.isBookmarked());
  }
}
