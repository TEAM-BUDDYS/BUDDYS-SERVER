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
   * 저장된 값이 {@code verification}과 완전히 일치할 때만 삭제한다(재발송으로 갱신된 코드는 지우지 않음).
   */
  void deleteIfMatches(UniversityVerification verification);
}
