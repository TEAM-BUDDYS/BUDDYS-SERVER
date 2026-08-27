package org.sopt.buddys.domain.verification.entity;

import java.security.SecureRandom;
import java.util.Base64;

public record UniversityVerification(
    Long userId,
    Long universityId,
    String email,
    String token
) {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final int TOKEN_RANDOM_BYTES = 32;

  public static UniversityVerification issue(Long userId, Long universityId, String email) {
    byte[] randomBytes = new byte[TOKEN_RANDOM_BYTES];
    SECURE_RANDOM.nextBytes(randomBytes);
    String token = userId + "." + TOKEN_ENCODER.encodeToString(randomBytes);
    return new UniversityVerification(userId, universityId, email, token);
  }
}
