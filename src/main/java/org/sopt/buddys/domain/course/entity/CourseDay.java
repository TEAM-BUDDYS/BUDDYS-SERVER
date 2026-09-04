package org.sopt.buddys.domain.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "course_day",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_day", columnNames = {"course_id", "day_number"})
    }
)
public class CourseDay {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @Column(name = "day_number", nullable = false)
  private Short dayNumber;

  @Column
  private LocalDate date;

  public CourseDay(Course course, Short dayNumber, LocalDate date) {
    this.course = course;
    this.dayNumber = dayNumber;
    this.date = date;
  }
}
