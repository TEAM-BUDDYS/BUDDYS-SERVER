package org.sopt.buddys.domain.verification.entity;

import java.security.SecureRandom;

public record UniversityVerification(
    Long userId,
    Long universityId,
    String email,
    String code
) {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final char[] CODE_ALPHABET =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
  private static final int CODE_LENGTH = 6;

  public static UniversityVerification issue(Long userId, Long universityId, String email) {
    StringBuilder code = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      code.append(CODE_ALPHABET[SECURE_RANDOM.nextInt(CODE_ALPHABET.length)]);
    }
    return new UniversityVerification(userId, universityId, email, code.toString());
  }
}
