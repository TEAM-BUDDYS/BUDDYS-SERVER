package org.sopt.buddys.domain.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
    @Schema(description = "댓글 내용", example = "저도 같이 가고 싶어요!")
    @NotBlank
    @Size(max = 100)
    String content
) {
}
