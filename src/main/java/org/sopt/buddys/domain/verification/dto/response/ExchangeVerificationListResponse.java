package org.sopt.buddys.domain.verification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationListResult;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationListResult.ExchangeVerificationSummaryResult;

public record ExchangeVerificationListResponse(
    @Schema(description = "서류 인증 신청 목록")
    List<ExchangeVerificationSummaryResponse> content,

    @Schema(description = "현재 페이지 번호. 0부터 시작합니다.", example = "0")
    int page,

    @Schema(description = "페이지 크기", example = "20")
    int size,

    @Schema(description = "다음 페이지 존재 여부", example = "false")
    boolean hasNext
) {

  public ExchangeVerificationListResponse {
    content = List.copyOf(content);
  }

  public static ExchangeVerificationListResponse from(ExchangeVerificationListResult result) {
    return new ExchangeVerificationListResponse(
        result.content().stream()
            .map(ExchangeVerificationSummaryResponse::from)
            .toList(),
        result.page(),
        result.size(),
        result.hasNext()
    );
  }

  public record ExchangeVerificationSummaryResponse(
      @Schema(description = "서류 인증 신청 ID", example = "1")
      Long verificationId,

      @Schema(description = "신청자 사용자 ID", example = "10")
      Long userId,

      @Schema(description = "신청자 닉네임", example = "지현")
      String nickname,

      @Schema(description = "신청 일시", example = "2026-08-30T14:20:00")
      LocalDateTime submittedAt,

      @Schema(description = "처리 상태", example = "PENDING")
      ExchangeVerificationStatus status
  ) {

    private static ExchangeVerificationSummaryResponse from(ExchangeVerificationSummaryResult result) {
      return new ExchangeVerificationSummaryResponse(
          result.verificationId(),
          result.userId(),
          result.nickname(),
          result.submittedAt(),
          result.status()
      );
    }
  }
}
