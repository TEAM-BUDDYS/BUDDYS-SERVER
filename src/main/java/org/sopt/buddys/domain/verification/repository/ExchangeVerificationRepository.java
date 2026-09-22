package org.sopt.buddys.domain.verification.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.sopt.buddys.domain.verification.entity.ExchangeVerification;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExchangeVerificationRepository extends JpaRepository<ExchangeVerification, Long> {

  Optional<ExchangeVerification> findByUserIdAndStatus(
      Long userId,
      ExchangeVerificationStatus status
  );

  @EntityGraph(attributePaths = "user")
  @Query("select verification from ExchangeVerification verification where verification.id = :id")
  Optional<ExchangeVerification> findByIdWithUser(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @EntityGraph(attributePaths = "user")
  @Query("select verification from ExchangeVerification verification where verification.id = :id")
  Optional<ExchangeVerification> findByIdWithUserForUpdate(@Param("id") Long id);

  @EntityGraph(attributePaths = "user")
  @Query("""
      select verification
      from ExchangeVerification verification
      order by verification.updatedAt desc, verification.id desc
      """)
  Slice<ExchangeVerification> findAllWithUser(Pageable pageable);

  @EntityGraph(attributePaths = "user")
  @Query("""
      select verification
      from ExchangeVerification verification
      where verification.status = :status
      order by verification.updatedAt desc, verification.id desc
      """)
  Slice<ExchangeVerification> findAllWithUserByStatus(
      @Param("status") ExchangeVerificationStatus status,
      Pageable pageable
  );
}
