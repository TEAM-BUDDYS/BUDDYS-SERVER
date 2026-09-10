package org.sopt.buddys.domain.verification.service.result;

import java.time.LocalDateTime;
import org.sopt.buddys.domain.verification.entity.ExchangeVerification;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;

public record ExchangeVerificationDetailResult(
    Long verificationId,
    Long userId,
    String nickname,
    LocalDateTime submittedAt,
    ExchangeVerificationStatus status,
    String originalFileName,
    String documentUrl,
    String rejectionReason
) {

  public static ExchangeVerificationDetailResult of(
      ExchangeVerification verification,
      String documentUrl
  ) {
    return new ExchangeVerificationDetailResult(
        verification.getId(),
        verification.getUser().getId(),
        verification.getUser().getNickname(),
        verification.getUpdatedAt(),
        verification.getStatus(),
        verification.getOriginalFileName(),
        documentUrl,
        verification.getRejectionReason()
    );
  }
}
