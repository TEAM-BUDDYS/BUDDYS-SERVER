package org.sopt.buddys.domain.course.entity;

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
public class CourseCompanionId implements Serializable {

  @Column(name = "course_id")
  private Long courseId;

  @Column(name = "user_id")
  private Long userId;

  public CourseCompanionId(Long courseId, Long userId) {
    this.courseId = courseId;
    this.userId = userId;
  }
}
