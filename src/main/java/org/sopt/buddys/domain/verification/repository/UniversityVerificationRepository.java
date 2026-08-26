package org.sopt.buddys.domain.verification.repository;

import java.util.Optional;
import org.sopt.buddys.domain.verification.entity.UniversityVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityVerificationRepository extends JpaRepository<UniversityVerification, Long> {
  Optional<UniversityVerification> findByToken(String token);
}
