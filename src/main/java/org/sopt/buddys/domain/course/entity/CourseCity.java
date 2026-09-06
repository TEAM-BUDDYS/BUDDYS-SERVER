package org.sopt.buddys.domain.course.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.buddys.domain.location.entity.City;
import org.springframework.data.domain.Persistable;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "course_city")
public class CourseCity implements Persistable<CourseCityId> {

  @EmbeddedId
  private CourseCityId id;

  @MapsId("courseId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @MapsId("cityId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "city_id", nullable = false)
  private City city;

  @Transient
  private boolean isNew;

  public CourseCity(Course course, City city) {
    this.course = course;
    this.city = city;
    this.id = new CourseCityId(course.getId(), city.getId());
    this.isNew = true;
  }

  @Override
  public CourseCityId getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  @PostPersist
  @PostLoad
  private void markNotNew() {
    this.isNew = false;
  }
}
