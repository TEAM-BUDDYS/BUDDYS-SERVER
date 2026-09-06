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
public class CourseTagId implements Serializable {

  @Column(name = "course_id")
  private Long courseId;

  @Column(name = "tag_id")
  private Long tagId;

  public CourseTagId(Long courseId, Long tagId) {
    this.courseId = courseId;
    this.tagId = tagId;
  }
}
