package org.sopt.buddys.domain.user.repository;

import java.util.Optional;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

  Optional<User> findByIdAndDeletedAtIsNull(Long id);
}
