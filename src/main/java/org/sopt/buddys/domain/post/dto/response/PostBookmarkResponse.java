package org.sopt.buddys.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.post.service.result.PostBookmarkResult;

public record PostBookmarkResponse(
    @Schema(description = "게시글 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long postId,
    @Schema(description = "게시글 저장 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean isBookmarked
) {
  public static PostBookmarkResponse from(PostBookmarkResult result) {
    return new PostBookmarkResponse(result.postId(), result.isBookmarked());
  }
}
