package org.sopt.buddys.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_token")
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, unique = true)
  private Long userId;

  @Column(nullable = false, length = 512)
  private String token;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  public static RefreshToken of(Long userId, String token, long refreshTokenExpiration) {
    RefreshToken rt = new RefreshToken();
    rt.userId = userId;
    rt.token = token;
    rt.expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000);
    return rt;
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(this.expiresAt);
  }
}
