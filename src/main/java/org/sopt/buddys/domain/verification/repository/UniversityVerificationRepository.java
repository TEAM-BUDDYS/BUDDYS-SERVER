package org.sopt.buddys.domain.verification.repository;

import java.time.Duration;
import java.util.Optional;
import org.sopt.buddys.domain.verification.entity.UniversityVerification;

public interface UniversityVerificationRepository {

  void save(UniversityVerification verification, Duration ttl);

  Optional<UniversityVerification> findByToken(String token);

  void deleteIfTokenMatches(UniversityVerification verification);
}
