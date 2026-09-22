package org.sopt.buddys.domain.verification.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;
import org.sopt.buddys.domain.verification.service.ExchangeVerificationNotificationMailSender;

class ExchangeVerificationReviewedEventListenerTest {

  private static final String RECIPIENT_EMAIL = "applicant@example.com";

  private ExchangeVerificationNotificationMailSender mailSender;
  private ExchangeVerificationReviewedEventListener listener;

  @BeforeEach
  void setUp() {
    mailSender = mock(ExchangeVerificationNotificationMailSender.class);
    listener = new ExchangeVerificationReviewedEventListener(mailSender);
  }

  @DisplayName("승인 이벤트가 커밋되면 승인 메일을 발송한다")
  @Test
  void handle_approved_sendsApprovedMail() {
    listener.handle(event(ExchangeVerificationStatus.APPROVED));

    verify(mailSender).sendApproved(RECIPIENT_EMAIL);
  }

  @DisplayName("반려 이벤트가 커밋되면 반려 메일을 발송한다")
  @Test
  void handle_rejected_sendsRejectedMail() {
    listener.handle(event(ExchangeVerificationStatus.REJECTED));

    verify(mailSender).sendRejected(RECIPIENT_EMAIL);
  }

  @DisplayName("메일 발송에 실패해도 관리자 승인 결과에는 영향을 주지 않는다")
  @Test
  void handle_mailFailure_doesNotPropagateException() {
    willThrow(new RuntimeException("SES failure"))
        .given(mailSender)
        .sendApproved(RECIPIENT_EMAIL);

    assertThatCode(() -> listener.handle(event(ExchangeVerificationStatus.APPROVED)))
        .doesNotThrowAnyException();
  }

  @DisplayName("검토 완료 상태가 아닌 이벤트에는 메일을 발송하지 않는다")
  @Test
  void handle_pending_doesNotSendMail() {
    listener.handle(event(ExchangeVerificationStatus.PENDING));

    verifyNoInteractions(mailSender);
  }

  private ExchangeVerificationReviewedEvent event(ExchangeVerificationStatus status) {
    return new ExchangeVerificationReviewedEvent(10L, RECIPIENT_EMAIL, status);
  }
}
