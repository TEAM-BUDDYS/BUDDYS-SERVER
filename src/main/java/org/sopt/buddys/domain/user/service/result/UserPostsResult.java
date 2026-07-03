package org.sopt.buddys.domain.user.service.result;

import java.util.List;
import org.sopt.buddys.domain.post.entity.Post;

public record UserPostsResult(
    List<PostResult> posts,
    int page,
    int size,
    boolean hasNext
) {

  public UserPostsResult {
    posts = List.copyOf(posts);
  }

  public record PostResult(
      Post post,
      String thumbnailImageUrl
  ) {
  }
}
