package org.sopt.buddys.domain.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.sopt.buddys.domain.location.entity.City;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.global.common.entity.BaseEntity;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post")
public class Post extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id", nullable = false)
  private User author;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "country_id", nullable = false)
  private Country country;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "city_id", nullable = false)
  private City city;

  // 입력 제한은 40자지만, 제한 변경 전 작성된 게시글이 있어 컬럼 길이는 120으로 유지한다.
  public static final int TITLE_MAX_LENGTH = 40;

  // 저장되는 값(앞뒤 공백 제거)을 기준으로, 이모지도 1글자로 센다.
  // MySQL VARCHAR 길이도 글자(코드 포인트) 단위라 컬럼 길이 계산과도 일치한다.
  public static boolean isTitleTooLong(String title) {
    String trimmed = title.trim();
    return trimmed.codePointCount(0, trimmed.length()) > TITLE_MAX_LENGTH;
  }

  @Column(nullable = false, length = 120)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "companion_type", nullable = false, length = 30)
  private CompanionType companionType;

  @Enumerated(EnumType.STRING)
  @Column(name = "recruitment_count_type", nullable = false, length = 20)
  private RecruitmentCountType recruitmentCountType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PostStatus status = PostStatus.RECRUITING;

  @Column(name = "view_count", nullable = false)
  private Long viewCount = 0L;

  @Column(name = "comment_count", nullable = false)
  private Long commentCount = 0L;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public Post(
      User author,
      Country country,
      City city,
      String title,
      String content,
      LocalDate startDate,
      LocalDate endDate,
      CompanionType companionType,
      RecruitmentCountType recruitmentCountType
  ) {
    this.author = author;
    this.country = country;
    this.city = city;
    this.title = title;
    this.content = content;
    this.startDate = startDate;
    this.endDate = endDate;
    this.companionType = companionType;
    this.recruitmentCountType = recruitmentCountType;
    this.status = PostStatus.RECRUITING;
    this.viewCount = 0L;
    this.commentCount = 0L;
  }

  public void updateStatus(PostStatus status) {
    this.status = status;
  }

  public void update(
      Country country, City city, LocalDate startDate, LocalDate endDate,
      String title, String content, CompanionType companionType,
      RecruitmentCountType recruitmentCountType
  ) {
    this.country = country;
    this.city = city;
    this.startDate = startDate;
    this.endDate = endDate;
    this.title = title;
    this.content = content;
    this.companionType = companionType;
    this.recruitmentCountType = recruitmentCountType;
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }

  public void softDelete(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }
}
