package org.sopt.buddys.domain.verification.repository;

import org.sopt.buddys.domain.verification.entity.ExchangeVerification;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeVerificationRepository extends JpaRepository<ExchangeVerification, Long> {

  @EntityGraph(attributePaths = "user")
  Slice<ExchangeVerification> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

  @EntityGraph(attributePaths = "user")
  Slice<ExchangeVerification> findAllByStatusOrderByCreatedAtDescIdDesc(
      ExchangeVerificationStatus status,
      Pageable pageable
  );
}
