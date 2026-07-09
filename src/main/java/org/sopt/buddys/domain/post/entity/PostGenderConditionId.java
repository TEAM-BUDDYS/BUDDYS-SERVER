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
public class PostGenderConditionId implements Serializable {

  @Column(name = "post_id")
  private Long postId;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender_condition", length = 10)
  private GenderCondition genderCondition;

  public PostGenderConditionId(Long postId, GenderCondition genderCondition) {
    this.postId = postId;
    this.genderCondition = genderCondition;
  }
}
