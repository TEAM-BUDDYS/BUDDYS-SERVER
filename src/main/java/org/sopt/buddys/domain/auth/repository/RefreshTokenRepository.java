package org.sopt.buddys.domain.auth.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.sopt.buddys.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token")
  Optional<RefreshToken> findByTokenForUpdate(@Param("token") String token);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId")
  void deleteByUserId(@Param("userId") Long userId);
}
