package org.sopt.buddys.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostStatus;

public record UpdatePostStatusResponse(
    @Schema(description = "게시글 ID", example = "1")
    Long postId,

    @Schema(description = "변경된 모집 상태", example = "COMPLETED")
    PostStatus status
) {

  public static UpdatePostStatusResponse from(Post post) {
    return new UpdatePostStatusResponse(
        post.getId(),
        post.getStatus()
    );
  }
}
