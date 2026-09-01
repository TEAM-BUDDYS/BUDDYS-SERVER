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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "course_image")
public class CourseImage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_day_id", nullable = false)
  private CourseDay courseDay;

  @Column(name = "image_url", nullable = false, length = 512)
  private String imageUrl;

  @Column(name = "order_no", nullable = false)
  private Short orderNo = 0;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public CourseImage(CourseDay courseDay, String imageUrl, Short orderNo) {
    this.courseDay = courseDay;
    this.imageUrl = imageUrl;
    this.orderNo = orderNo;
  }
}
