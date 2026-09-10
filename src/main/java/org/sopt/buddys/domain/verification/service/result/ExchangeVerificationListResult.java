package org.sopt.buddys.domain.verification.service.result;

import java.time.LocalDateTime;
import java.util.List;
import org.sopt.buddys.domain.verification.entity.ExchangeVerification;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;

public record ExchangeVerificationListResult(
    List<ExchangeVerificationSummaryResult> content,
    int page,
    int size,
    boolean hasNext
) {

  public ExchangeVerificationListResult {
    content = List.copyOf(content);
  }

  public record ExchangeVerificationSummaryResult(
      Long verificationId,
      Long userId,
      String nickname,
      LocalDateTime submittedAt,
      ExchangeVerificationStatus status
  ) {

    public static ExchangeVerificationSummaryResult from(ExchangeVerification verification) {
      return new ExchangeVerificationSummaryResult(
          verification.getId(),
          verification.getUser().getId(),
          verification.getUser().getNickname(),
          verification.getUpdatedAt(),
          verification.getStatus()
      );
    }
  }
}
