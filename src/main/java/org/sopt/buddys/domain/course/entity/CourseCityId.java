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
public class CourseCityId implements Serializable {

  @Column(name = "course_id")
  private Long courseId;

  @Column(name = "city_id")
  private Long cityId;

  public CourseCityId(Long courseId, Long cityId) {
    this.courseId = courseId;
    this.cityId = cityId;
  }
}
