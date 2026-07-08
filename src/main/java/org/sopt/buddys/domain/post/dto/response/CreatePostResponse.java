package org.sopt.buddys.domain.post.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.post.entity.Post;

public record CreatePostResponse(
    @Schema(description = "생성된 게시글 ID", example = "1")
    Long postId
) {

  public static CreatePostResponse from(Post post) {
    return new CreatePostResponse(post.getId());
  }
}
