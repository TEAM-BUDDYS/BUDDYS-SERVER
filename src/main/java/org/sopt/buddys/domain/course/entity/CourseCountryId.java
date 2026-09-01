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
public class CourseCountryId implements Serializable {

  @Column(name = "course_id")
  private Long courseId;

  @Column(name = "country_id")
  private Long countryId;

  public CourseCountryId(Long courseId, Long countryId) {
    this.courseId = courseId;
    this.countryId = countryId;
  }
}
