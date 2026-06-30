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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sopt.buddys.domain.location.entity.City;
import org.sopt.buddys.domain.location.entity.Continent;
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

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private Continent continent;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "country_id", nullable = false)
  private Country country;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "city_id")
  private City city;

  @Column(nullable = false, length = 120)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "recruit_count", nullable = false)
  private Short recruitCount = 1;

  @Column(name = "min_age")
  private Short minAge;

  @Column(name = "max_age")
  private Short maxAge;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender_condition", nullable = false, length = 10)
  private GenderCondition genderCondition = GenderCondition.ANY;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PostStatus status = PostStatus.RECRUITING;

  @Column(name = "view_count", nullable = false)
  private Long viewCount = 0L;

  @Column(name = "comment_count", nullable = false)
  private Long commentCount = 0L;
}