package org.sopt.buddys.domain.auth.repository;

import java.util.Optional;
import org.sopt.buddys.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByToken(String token);

  @Modifying
  @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId")
  void deleteByUserId(@Param("userId") Long userId);
}
