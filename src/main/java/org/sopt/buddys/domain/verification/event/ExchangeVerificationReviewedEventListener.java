package org.sopt.buddys.domain.verification.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.domain.verification.service.ExchangeVerificationNotificationMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeVerificationReviewedEventListener {

  private final ExchangeVerificationNotificationMailSender mailSender;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ExchangeVerificationReviewedEvent event) {
    try {
      switch (event.status()) {
        case APPROVED -> mailSender.sendApproved(event.recipientEmail());
        case REJECTED -> mailSender.sendRejected(event.recipientEmail());
        default -> log.warn(
            "[ExchangeVerification] unsupported reviewed event status verificationId={}, status={}",
            event.verificationId(),
            event.status()
        );
      }
    } catch (RuntimeException exception) {
      log.error(
          "[ExchangeVerification] result mail sending failed verificationId={}, status={}",
          event.verificationId(),
          event.status(),
          exception
      );
    }
  }
}
