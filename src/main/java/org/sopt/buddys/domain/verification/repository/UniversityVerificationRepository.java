package org.sopt.buddys.domain.verification.repository;

import java.time.Duration;
import java.util.Optional;
import org.sopt.buddys.domain.verification.entity.UniversityVerification;

public interface UniversityVerificationRepository {

  void save(UniversityVerification verification, Duration ttl);

  /**
   * 사용자에게 저장된 코드가 {@code code}와 일치하면 해당 인증 정보를 반환한다. 없거나 불일치하면 비어 있다.
   */
  Optional<UniversityVerification> findByUserIdAndCode(Long userId, String code);

  /**
   * 실패 시도 횟수를 1 증가시키고 증가 후 값을 반환한다. 최초 증가 시 {@code ttl}로 만료를 설정한다.
   */
  long incrementAttemptCount(Long userId, Duration ttl);

  /**
   * 저장된 값이 {@code verification}과 완전히 일치할 때만 삭제한다(재발송으로 갱신된 코드는 지우지 않음).
   */
  void deleteIfMatches(UniversityVerification verification);

  /**
   * 사용자의 인증 정보와 실패 시도 횟수를 조건 없이 삭제한다.
   */
  void deleteByUserId(Long userId);
}
