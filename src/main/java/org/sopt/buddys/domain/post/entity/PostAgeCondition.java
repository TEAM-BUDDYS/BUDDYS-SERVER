package org.sopt.buddys.domain.post.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_age_condition")
public class PostAgeCondition {

  @EmbeddedId
  private PostAgeConditionId id;

  @MapsId("postId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  public PostAgeCondition(Post post, AgeCondition ageCondition) {
    this.post = post;
    this.id = new PostAgeConditionId(post.getId(), ageCondition);
  }
}
