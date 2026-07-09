package org.sopt.buddys.global.aws.s3;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class S3PresignedUrlManager {

  private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofMinutes(5);

  private final S3Presigner s3Presigner;
  private final S3Utilities s3Utilities;
  private final S3Properties s3Properties;

  public S3PresignedUploadResult createUploadUrl(String key, String contentType) {
    PutObjectRequest objectRequest = PutObjectRequest.builder()
        .bucket(s3Properties.getBucket())
        .key(key)
        .contentType(contentType)
        .build();

    PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(
        PutObjectPresignRequest.builder()
            .signatureDuration(PRESIGNED_URL_EXPIRATION)
            .putObjectRequest(objectRequest)
            .build()
    );

    return new S3PresignedUploadResult(presigned.url().toString(), buildPublicUrl(key));
  }

  private String buildPublicUrl(String key) {
    return s3Utilities.getUrl(GetUrlRequest.builder()
            .bucket(s3Properties.getBucket())
            .key(key)
            .build())
        .toString();
  }
}