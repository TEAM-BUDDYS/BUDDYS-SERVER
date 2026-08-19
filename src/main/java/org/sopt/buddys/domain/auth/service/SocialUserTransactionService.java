package org.sopt.buddys.domain.auth.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocialUserTransactionService {

  private final UserRepository userRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public User create(User user) {
    return userRepository.saveAndFlush(user);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Optional<User> findByProviderAndProviderId(
      AuthProvider provider,
      String providerId
  ) {
    return userRepository.findByProviderAndProviderId(provider, providerId);
  }
}
