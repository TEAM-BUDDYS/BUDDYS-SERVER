package org.sopt.buddys.domain.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.sopt.buddys.domain.post.entity.PostStatus;

public record UpdatePostStatusRequest(
    @Schema(description = "변경할 모집 상태", example = "COMPLETED")
    @NotNull
    PostStatus status
) {
}
