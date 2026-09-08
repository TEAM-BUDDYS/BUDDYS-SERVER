package org.sopt.buddys.domain.verification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.verification.service.result.ExchangeDocumentUploadUrlResult;

public record ExchangeDocumentUploadUrlResponse(
    @Schema(
        description = "클라이언트가 서류를 PUT할 presigned URL. 발급 후 5분간 유효합니다.",
        example = "https://buddys-assets.s3.ap-northeast-2.amazonaws.com/"
            + "exchange-verifications/1/123e4567-e89b-12d3-a456-426614174000.pdf?X-Amz-Algorithm=..."
    )
    String uploadUrl,

    @Schema(
        description = "업로드 요청마다 사용자 경로에 새로 생성되며, 인증 신청에 사용할 고유 S3 객체 키",
        example = "exchange-verifications/1/123e4567-e89b-12d3-a456-426614174000.pdf"
    )
    String documentKey
) {

  public static ExchangeDocumentUploadUrlResponse from(ExchangeDocumentUploadUrlResult result) {
    return new ExchangeDocumentUploadUrlResponse(result.uploadUrl(), result.documentKey());
  }
}
