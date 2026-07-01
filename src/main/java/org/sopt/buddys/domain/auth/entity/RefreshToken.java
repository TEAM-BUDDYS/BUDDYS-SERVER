package org.sopt.buddys.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_token", uniqueConstraints = {
    @UniqueConstraint(name = "uk_refresh_token_token", columnNames = "token")
})
public class RefreshToken implements Persistable<Long> {

  @Id
  private Long userId;

  @Column(nullable = false, length = 512)
  private String token;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Transient
  private boolean isNew;

  @Override
  public Long getId() {
    return userId;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  public static RefreshToken of(Long userId, String token, long refreshTokenExpiration) {
    RefreshToken rt = new RefreshToken();
    rt.userId = userId;
    rt.token = token;
    rt.expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000);
    rt.isNew = true;
    return rt;
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(this.expiresAt);
  }
}
