package org.sopt.buddys.domain.verification.event;

import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;

public record ExchangeVerificationReviewedEvent(
    Long verificationId,
    String recipientEmail,
    ExchangeVerificationStatus status
) {
}
