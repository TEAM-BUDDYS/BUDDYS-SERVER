package org.sopt.buddys.domain.verification.repository;

import java.time.Duration;
import org.sopt.buddys.domain.verification.entity.UniversityVerification;

public interface UniversityVerificationRepository {

  void save(UniversityVerification verification, Duration ttl);

  /**
   * 사용자에게 저장된 코드가 {@code code}와 일치하면 해당 인증 정보를 반환한다.
   * 코드가 다르면 실패 횟수를 증가시키고, {@code maxAttempts}에 도달하면 인증 정보를 폐기한다.
   */
  VerificationResult verifyCode(Long userId, String code, int maxAttempts);

  /**
   * 저장된 값이 {@code verification}과 완전히 일치할 때만 삭제한다(재발송으로 갱신된 코드는 지우지 않음).
   */
  void deleteIfMatches(UniversityVerification verification);

  record VerificationResult(Status status, UniversityVerification verification) {

    public static VerificationResult matched(UniversityVerification verification) {
      return new VerificationResult(Status.MATCHED, verification);
    }

    public static VerificationResult invalid() {
      return new VerificationResult(Status.INVALID, null);
    }

    public static VerificationResult limitExceeded() {
      return new VerificationResult(Status.ATTEMPT_LIMIT_EXCEEDED, null);
    }
  }

  enum Status {
    MATCHED,
    INVALID,
    ATTEMPT_LIMIT_EXCEEDED
  }
}
