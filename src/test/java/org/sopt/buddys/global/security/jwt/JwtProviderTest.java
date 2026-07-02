package org.sopt.buddys.global.security.jwt;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class JwtProviderTest {

  private JwtProvider jwtProvider;
  private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes-long!!";
  private static final long EXPIRATION = 3600000; // 1 hour

  @BeforeEach
  void setUp() {
    JwtProperties properties = new JwtProperties(SECRET, EXPIRATION, 0L);
    jwtProvider = new JwtProvider(properties);
  }

  @DisplayName("유효한 userId로 토큰을 생성하면 해당 userId를 포함한 JWT가 발급된다")
  @Test
  void generateToken() {
    // given
    Long userId = 1L;

    // when
    String token = jwtProvider.generateToken(userId);

    // then
    assertThat(token).isNotBlank();
    assertThat(jwtProvider.getUserId(token)).isEqualTo(userId);
  }

  @DisplayName("유효한 JWT는 검증에 성공한다")
  @Test
  void validateToken_success() {
    // given
    String token = jwtProvider.generateToken(1L);

    // when
    boolean result = jwtProvider.validateToken(token);

    // then
    assertThat(result).isTrue();
  }

  @DisplayName("변조된 JWT는 검증에 실패한다")
  @Test
  void validateToken_fail_withTamperedToken() {
    // given
    String token = jwtProvider.generateToken(1L) + "tampered";

    // when
    boolean result = jwtProvider.validateToken(token);

    // then
    assertThat(result).isFalse();
  }

  @DisplayName("만료된 JWT는 검증에 실패한다")
  @Test
  void validateToken_fail_withExpiredToken() {
    // given
    JwtProperties expiredProperties = new JwtProperties(SECRET, -1L, 0L); // 즉시 만료
    JwtProvider expiredProvider = new JwtProvider(expiredProperties);
    String token = expiredProvider.generateToken(1L);

    // when
    boolean result = expiredProvider.validateToken(token);

    // then
    assertThat(result).isFalse();
  }

}
