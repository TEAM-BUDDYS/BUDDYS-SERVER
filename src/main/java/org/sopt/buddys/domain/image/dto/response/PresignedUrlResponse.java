package org.sopt.buddys.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.global.aws.s3.S3PresignedUploadResult;

public record PresignedUrlResponse(
    @Schema(
        description = "클라이언트가 파일을 PUT할 presigned URL. 발급 후 5분간만 유효합니다.",
        example = "https://buddys-assets.s3.ap-northeast-2.amazonaws.com/posts/3f1e...jpg?X-Amz-Algorithm=..."
    )
    String uploadUrl,

    @Schema(
        description = "업로드 완료 후 게시글/프로필 등록 시 사용할 최종 이미지 URL",
        example = "https://buddys-assets.s3.ap-northeast-2.amazonaws.com/posts/3f1e....jpg"
    )
    String imageUrl
) {

  public static PresignedUrlResponse from(S3PresignedUploadResult result) {
    return new PresignedUrlResponse(result.uploadUrl(), result.imageUrl());
  }
}