package org.sopt.buddys.domain.verification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExchangeVerificationRejectRequest(
    @Schema(description = "반려 사유", example = "서류가 확인되지 않습니다.")
    @NotBlank
    @Size(max = 500)
    String rejectionReason
) {
}
