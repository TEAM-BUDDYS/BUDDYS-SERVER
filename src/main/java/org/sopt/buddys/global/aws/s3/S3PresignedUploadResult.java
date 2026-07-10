package org.sopt.buddys.global.aws.s3;

public record S3PresignedUploadResult(
    String uploadUrl,
    String imageUrl
) {
}