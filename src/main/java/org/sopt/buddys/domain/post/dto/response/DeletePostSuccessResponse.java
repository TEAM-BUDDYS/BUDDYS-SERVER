package org.sopt.buddys.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 삭제 성공 응답")
public record DeletePostSuccessResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    boolean success,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "POST-S005")
    String code,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "게시글 삭제에 성공했습니다.")
    String message,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    DeletePostResponse data
) {
}
