package org.sopt.buddys.domain.verification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.verification.service.result.ExchangeDocumentUploadUrlResult;

public record ExchangeDocumentUploadUrlResponse(
    @Schema(
        description = "클라이언트가 서류를 PUT할 presigned URL. 발급 후 5분간 유효합니다.",
        example = "https://buddys-assets.s3.ap-northeast-2.amazonaws.com/exchange/1/uuid.pdf?X-Amz-Algorithm=..."
    )
    String uploadUrl,

    @Schema(
        description = "업로드 완료 후 파견교 인증 신청에 사용할 S3 객체 키",
        example = "exchange/1/550e8400-e29b-41d4-a716-446655440000.pdf"
    )
    String documentKey
) {

  public static ExchangeDocumentUploadUrlResponse from(ExchangeDocumentUploadUrlResult result) {
    return new ExchangeDocumentUploadUrlResponse(result.uploadUrl(), result.documentKey());
  }
}
