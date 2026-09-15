package org.sopt.buddys.domain.verification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.domain.user.event.UserWithdrawnEvent;
import org.sopt.buddys.domain.verification.repository.UniversityVerificationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserWithdrawalVerificationListener {

  private final UniversityVerificationRepository universityVerificationRepository;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void deleteUniversityVerification(UserWithdrawnEvent event) {
    try {
      universityVerificationRepository.deleteByUserId(event.userId());
    } catch (RuntimeException exception) {
      log.warn(
          "Failed to delete university verification after user withdrawal. userId={}",
          event.userId(),
          exception
      );
    }
  }
}
