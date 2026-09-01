package org.sopt.buddys.domain.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostBookmarkId implements Serializable {

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "post_id")
  private Long postId;

  public PostBookmarkId(Long userId, Long postId) {
    this.userId = userId;
    this.postId = postId;
  }
}
