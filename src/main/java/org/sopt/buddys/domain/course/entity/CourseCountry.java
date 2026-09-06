package org.sopt.buddys.domain.course.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.buddys.domain.location.entity.Country;
import org.springframework.data.domain.Persistable;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "course_country")
public class CourseCountry implements Persistable<CourseCountryId> {

  @EmbeddedId
  private CourseCountryId id;

  @MapsId("courseId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @MapsId("countryId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "country_id", nullable = false)
  private Country country;

  @Transient
  private boolean isNew;

  public CourseCountry(Course course, Country country) {
    this.course = course;
    this.country = country;
    this.id = new CourseCountryId(course.getId(), country.getId());
    this.isNew = true;
  }

  @Override
  public CourseCountryId getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }
}
