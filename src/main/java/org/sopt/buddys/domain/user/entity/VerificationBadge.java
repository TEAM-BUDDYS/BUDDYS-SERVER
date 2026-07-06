package org.sopt.buddys.domain.user.entity;

public enum VerificationBadge {
  SOCIAL_LOGIN,
  UNIVERSITY_VERIFIED,
  EXCHANGE_VERIFIED;

  public static VerificationBadge from(User user) {
    if (user.isExchangeVerified()) {
      return EXCHANGE_VERIFIED;
    }
    if (user.isUniversityVerified()) {
      return UNIVERSITY_VERIFIED;
    }
    return SOCIAL_LOGIN;
  }
}
