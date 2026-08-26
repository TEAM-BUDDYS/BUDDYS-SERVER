package org.sopt.buddys.domain.verification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "university_verification", uniqueConstraints = {
    @UniqueConstraint(name = "uk_university_verification_token", columnNames = "token")
})
public class UniversityVerification implements Persistable<Long> {

  @Id
  private Long userId;

  @Column(name = "university_id", nullable = false)
  private Long universityId;

  @Column(nullable = false, length = 255)
  private String email;

  @Column(nullable = false, length = 255)
  private String token;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Transient
  private boolean isNew;

  public static UniversityVerification of(Long userId, Long universityId, String email, long tokenExpiration) {
    UniversityVerification verification = new UniversityVerification();
    verification.userId = userId;
    verification.universityId = universityId;
    verification.email = email;
    verification.token = UUID.randomUUID().toString().replace("-", "");
    verification.expiresAt = LocalDateTime.now().plusSeconds(tokenExpiration / 1000);
    verification.isNew = true;
    return verification;
  }

  @Override
  public Long getId() {
    return userId;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(this.expiresAt);
  }
}
