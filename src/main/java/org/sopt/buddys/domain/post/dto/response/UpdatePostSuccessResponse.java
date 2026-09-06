package org.sopt.buddys.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 수정 성공 응답")
public record UpdatePostSuccessResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    boolean success,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "POST-S004")
    String code,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "게시글 수정에 성공했습니다.")
    String message,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    UpdatePostResponse data
) {
}
