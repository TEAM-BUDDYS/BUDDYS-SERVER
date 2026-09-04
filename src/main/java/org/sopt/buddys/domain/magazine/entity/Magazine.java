package org.sopt.buddys.domain.magazine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.buddys.global.common.entity.BaseEntity;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "magazine")
public class Magazine extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String title;

  @Column(nullable = false, length = 255)
  private String summary;

  @Column(name = "thumbnail_image_url", nullable = false, length = 512)
  private String thumbnailImageUrl;

  @Column(name = "external_url", nullable = false, length = 512)
  private String externalUrl;

  @Column(name = "published_at", nullable = false)
  private LocalDate publishedAt;

  public Magazine(
      String title,
      String summary,
      String thumbnailImageUrl,
      String externalUrl,
      LocalDate publishedAt
  ) {
    this.title = title;
    this.summary = summary;
    this.thumbnailImageUrl = thumbnailImageUrl;
    this.externalUrl = externalUrl;
    this.publishedAt = publishedAt;
  }
}
