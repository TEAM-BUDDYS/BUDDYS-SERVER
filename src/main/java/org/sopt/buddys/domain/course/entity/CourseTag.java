package org.sopt.buddys.domain.course.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.buddys.domain.tag.entity.Tag;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "course_tag")
public class CourseTag {

  @EmbeddedId
  private CourseTagId id;

  @MapsId("courseId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @MapsId("tagId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tag_id", nullable = false)
  private Tag tag;

  public CourseTag(Course course, Tag tag) {
    this.course = course;
    this.tag = tag;
    this.id = new CourseTagId(course.getId(), tag.getId());
  }
}
