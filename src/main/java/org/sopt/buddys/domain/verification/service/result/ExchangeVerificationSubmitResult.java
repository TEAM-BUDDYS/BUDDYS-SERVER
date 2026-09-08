package org.sopt.buddys.domain.verification.service.result;

import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;

public record ExchangeVerificationSubmitResult(
    Long verificationId,
    ExchangeVerificationStatus status
) {
}
