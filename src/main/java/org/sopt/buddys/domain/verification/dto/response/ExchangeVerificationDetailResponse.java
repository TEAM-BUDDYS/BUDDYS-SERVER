package org.sopt.buddys.domain.verification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationDetailResult;

public record ExchangeVerificationDetailResponse(
    @Schema(description = "서류 인증 신청 ID", example = "1")
    Long verificationId,

    @Schema(description = "신청자 사용자 ID", example = "10")
    Long userId,

    @Schema(description = "신청자 닉네임", example = "지현")
    String nickname,

    @Schema(description = "신청 일시(UTC)", example = "2026-08-30T05:20:00Z")
    OffsetDateTime submittedAt,

    @Schema(description = "처리 상태", example = "PENDING")
    ExchangeVerificationStatus status,

    @Schema(description = "업로드한 원본 파일명", example = "교환학생 확인서.pdf")
    String originalFileName,

    @Schema(description = "서류 열람용 URL. 5분 동안 유효합니다.")
    String documentUrl,

    @Schema(description = "반려 사유. 반려 상태가 아니면 null입니다.", nullable = true,
        example = "서류가 확인되지 않습니다.")
    String rejectionReason
) {

  private static final ZoneId STORAGE_ZONE = ZoneId.of("Asia/Seoul");

  public static ExchangeVerificationDetailResponse from(ExchangeVerificationDetailResult result) {
    return new ExchangeVerificationDetailResponse(
        result.verificationId(),
        result.userId(),
        result.nickname(),
        result.submittedAt()
            .atZone(STORAGE_ZONE)
            .withZoneSameInstant(ZoneOffset.UTC)
            .toOffsetDateTime(),
        result.status(),
        result.originalFileName(),
        result.documentUrl(),
        result.rejectionReason()
    );
  }
}
