package org.sopt.buddys.domain.verification.repository;

import java.util.Optional;
import org.sopt.buddys.domain.verification.entity.ExchangeVerification;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExchangeVerificationRepository extends JpaRepository<ExchangeVerification, Long> {

  Optional<ExchangeVerification> findFirstByUserIdOrderByIdDesc(Long userId);

  @EntityGraph(attributePaths = "user")
  @Query("""
      select verification
      from ExchangeVerification verification
      where verification.id = (
        select max(latest.id)
        from ExchangeVerification latest
        where latest.user.id = verification.user.id
      )
      order by verification.updatedAt desc, verification.id desc
      """)
  Slice<ExchangeVerification> findLatestByUser(Pageable pageable);

  @EntityGraph(attributePaths = "user")
  @Query("""
      select verification
      from ExchangeVerification verification
      where verification.id = (
        select max(latest.id)
        from ExchangeVerification latest
        where latest.user.id = verification.user.id
      )
        and verification.status = :status
      order by verification.updatedAt desc, verification.id desc
      """)
  Slice<ExchangeVerification> findLatestByUserAndStatus(
      @Param("status") ExchangeVerificationStatus status,
      Pageable pageable
  );
}
