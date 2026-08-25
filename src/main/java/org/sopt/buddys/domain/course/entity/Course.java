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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.global.common.entity.BaseEntity;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "course")
public class Course extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id", nullable = false)
  private User author;

  @Column(nullable = false, length = 120)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String content;

  @Column(name = "thumbnail_image_url", length = 512)
  private String thumbnailImageUrl;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "view_count", nullable = false)
  private Long viewCount = 0L;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public Course(
      User author,
      String title,
      String content,
      String thumbnailImageUrl,
      LocalDate startDate,
      LocalDate endDate
  ) {
    this.author = author;
    this.title = title;
    this.content = content;
    this.thumbnailImageUrl = thumbnailImageUrl;
    this.startDate = startDate;
    this.endDate = endDate;
    this.viewCount = 0L;
  }

  public void delete() {
    this.deletedAt = LocalDateTime.now();
  }
}
