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
public class PostTagId implements Serializable {

  @Column(name = "post_id")
  private Long postId;

  @Column(name = "tag_id")
  private Long tagId;

  public PostTagId(Long postId, Long tagId) {
    this.postId = postId;
    this.tagId = tagId;
  }
}