package org.sopt.buddys.domain.verification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationSubmitResult;

public record ExchangeVerificationSubmitResponse(
    @Schema(description = "인증 신청 ID", example = "12")
    Long verificationId,

    @Schema(description = "처리 상태", example = "PENDING")
    ExchangeVerificationStatus status
) {

  public static ExchangeVerificationSubmitResponse from(ExchangeVerificationSubmitResult result) {
    return new ExchangeVerificationSubmitResponse(result.verificationId(), result.status());
  }
}
