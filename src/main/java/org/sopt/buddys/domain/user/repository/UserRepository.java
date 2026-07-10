package org.sopt.buddys.domain.user.repository;

import java.util.List;
import java.util.Optional;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

  Optional<User> findByIdAndDeletedAtIsNull(Long id);

  @Query("""
      select u
      from User u
      left join fetch u.exchangeCountry
      where u.id = :userId
        and u.deletedAt is null
      """)
  Optional<User> findByIdWithExchangeCountry(@Param("userId") Long userId);

  boolean existsByIdAndDeletedAtIsNull(Long id);

  @Query("""
      select u
      from User u
      join fetch u.exchangeCountry exchangeCountry
      where exchangeCountry.id = :exchangeCountryId
        and u.id <> :excludeUserId
        and u.deletedAt is null
      """)
  List<User> findByExchangeCountryIdWithExchangeCountry(
      @Param("exchangeCountryId") Long exchangeCountryId,
      @Param("excludeUserId") Long excludeUserId
  );
}
