package org.sopt.buddys.domain.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostAgeConditionId implements Serializable {

  @Column(name = "post_id")
  private Long postId;

  @Enumerated(EnumType.STRING)
  @Column(name = "age_condition", length = 20)
  private AgeCondition ageCondition;

  public PostAgeConditionId(Long postId, AgeCondition ageCondition) {
    this.postId = postId;
    this.ageCondition = ageCondition;
  }
}
