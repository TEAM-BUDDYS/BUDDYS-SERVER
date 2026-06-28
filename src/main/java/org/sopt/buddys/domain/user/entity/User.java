package org.sopt.buddys.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.sopt.buddys.domain.auth.code.AuthErrorCode;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.global.security.oauth.dto.KakaoUserInfo;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "`user`",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_nickname", columnNames = "nickname"),
        @UniqueConstraint(name = "uk_user_provider", columnNames = {"provider", "provider_id"})
    }
)
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private AuthProvider provider;

  @Column(name = "provider_id", nullable = false)
  private String providerId;

  @Column(nullable = false, length = 30)
  private String nickname;

  @Column(name = "profile_image_url", length = 512)
  private String profileImageUrl;

  @Column(length = 150)
  private String introduction;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Enumerated(EnumType.STRING)
  @Column(length = 10)
  private Gender gender;

  @Column(name = "notification_enabled", nullable = false)
  private Boolean notificationEnabled = true;

  @Enumerated(EnumType.STRING)
  @Column(name = "account_status", nullable = false, length = 20)
  private AccountStatus accountStatus = AccountStatus.ACTIVE;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exchange_country_id")
  private Country exchangeCountry;

  @Column(name = "exchange_university", length = 100)
  private String exchangeUniversity;

  @Column(name = "exchange_start_date")
  private LocalDate exchangeStartDate;

  @Column(name = "exchange_end_date")
  private LocalDate exchangeEndDate;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Builder
  private User(Long id, String email, AuthProvider provider, String providerId, String nickname, String profileImageUrl, String introduction,
      LocalDate birthDate, Gender gender, Boolean notificationEnabled, AccountStatus accountStatus, Country exchangeCountry,
      String exchangeUniversity, LocalDate exchangeStartDate, LocalDate exchangeEndDate, LocalDateTime deletedAt) {
    this.id = id;
    this.email = email;
    this.provider = provider;
    this.providerId = providerId;
    this.nickname = nickname;
    this.profileImageUrl = profileImageUrl;
    this.introduction = introduction;
    this.birthDate = birthDate;
    this.gender = gender;
    this.notificationEnabled = notificationEnabled != null ? notificationEnabled : true;
    this.accountStatus = accountStatus != null ? accountStatus : AccountStatus.ACTIVE;
    this.exchangeCountry = exchangeCountry;
    this.exchangeUniversity = exchangeUniversity;
    this.exchangeStartDate = exchangeStartDate;
    this.exchangeEndDate = exchangeEndDate;
    this.deletedAt = deletedAt;
  }

  public static User ofKakao(String providerId, KakaoUserInfo info) {
    KakaoUserInfo.KakaoAccount account = info.kakaoAccount();
    if (account == null || account.email() == null || account.email().isBlank()) {
      throw new org.sopt.buddys.global.exception.BaseException(AuthErrorCode.KAKAO_EMAIL_REQUIRED);
    }

    String nickname = (account.profile() != null && account.profile().nickname() != null && !account.profile().nickname().isBlank())
        ? account.profile().nickname()
        : "kakao_" + providerId.substring(Math.max(0, providerId.length() - 8));

    String profileImageUrl = (account.profile() != null) ? account.profile().profileImageUrl() : null;

    User user = new User();
    user.provider = AuthProvider.KAKAO;
    user.providerId = providerId;
    user.email = account.email();
    user.nickname = nickname;
    user.profileImageUrl = profileImageUrl;
    user.notificationEnabled = true;
    user.accountStatus = AccountStatus.ACTIVE;
    return user;
  }
}
