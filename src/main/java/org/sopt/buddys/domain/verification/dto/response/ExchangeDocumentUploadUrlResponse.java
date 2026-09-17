package org.sopt.buddys.domain.verification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import org.sopt.buddys.domain.verification.service.result.ExchangeDocumentUploadUrlResult;

public record ExchangeDocumentUploadUrlResponse(
    @Schema(
        description = "서류를 multipart/form-data로 POST할 S3 버킷 URL. 발급 후 5분간 유효합니다.",
        example = "https://buddys-assets.s3.ap-northeast-2.amazonaws.com/"
    )
    String uploadUrl,

    @Schema(description = "S3 POST 폼에 그대로 포함할 필드. 모든 필드를 먼저 넣고 file을 마지막에 추가하세요.")
    Map<String, String> fields,

    @Schema(
        description = "업로드 요청마다 사용자 경로에 새로 생성되며, 인증 신청에 사용할 고유 S3 객체 키",
        example = "exchange-verifications/1/123e4567-e89b-12d3-a456-426614174000.pdf"
    )
    String documentKey
) {

  public static ExchangeDocumentUploadUrlResponse from(ExchangeDocumentUploadUrlResult result) {
    return new ExchangeDocumentUploadUrlResponse(
        result.uploadUrl(), result.fields(), result.documentKey()
    );
  }
}
