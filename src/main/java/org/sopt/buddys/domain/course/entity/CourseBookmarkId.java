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
public class CourseBookmarkId implements Serializable {

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "course_id")
  private Long courseId;

  public CourseBookmarkId(Long userId, Long courseId) {
    this.userId = userId;
    this.courseId = courseId;
  }
}
