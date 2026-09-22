package org.sopt.buddys.global.aws.s3;

public record S3ObjectMetadata(
    String contentType,
    long contentLength
) {
}
