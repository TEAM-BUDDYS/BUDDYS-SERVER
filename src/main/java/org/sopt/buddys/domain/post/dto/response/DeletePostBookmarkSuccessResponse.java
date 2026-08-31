package org.sopt.buddys.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 저장 취소 성공 응답")
public record DeletePostBookmarkSuccessResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    boolean success,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "POST-S007")
    String code,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "게시글 저장 취소에 성공했습니다.")
    String message,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    PostBookmarkResponse data
) {
}
