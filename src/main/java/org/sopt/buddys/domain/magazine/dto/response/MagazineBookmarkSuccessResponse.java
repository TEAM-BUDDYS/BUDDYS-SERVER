package org.sopt.buddys.domain.magazine.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@Schema(description = "매거진 저장 성공 응답")
public record MagazineBookmarkSuccessResponse(
    @Schema(requiredMode = RequiredMode.REQUIRED, example = "true")
    boolean success,
    @Schema(requiredMode = RequiredMode.REQUIRED, example = "MAGAZINE-S002")
    String code,
    @Schema(requiredMode = RequiredMode.REQUIRED, example = "매거진 저장에 성공했습니다.")
    String message,
    @Schema(requiredMode = RequiredMode.REQUIRED)
    MagazineBookmarkResponse data
) {
}
