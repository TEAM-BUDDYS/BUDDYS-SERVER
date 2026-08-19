package org.sopt.buddys.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.post.entity.Post;

public record UpdatePostResponse(
    @Schema(description = "수정된 게시글 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long postId
) {
  public static UpdatePostResponse from(Post post) {
    return new UpdatePostResponse(post.getId());
  }
}
