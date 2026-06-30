package org.sopt.buddys.domain.user.entity;

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
public class UserTagId implements Serializable {

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "tag_id")
  private Long tagId;
}