package org.sopt.buddys.global.aws.s3;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@RequiredArgsConstructor
public class S3ObjectManager {

  private final S3Client s3Client;
  private final S3Properties s3Properties;

  public Optional<S3ObjectMetadata> findMetadata(String key) {
    try {
      HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
          .bucket(s3Properties.getBucket())
          .key(key)
          .build());
      return Optional.of(new S3ObjectMetadata(response.contentType(), response.contentLength()));
    } catch (S3Exception exception) {
      if (exception.statusCode() == 404) {
        return Optional.empty();
      }
      throw exception;
    }
  }

  public void delete(String key) {
    s3Client.deleteObject(DeleteObjectRequest.builder()
        .bucket(s3Properties.getBucket())
        .key(key)
        .build());
  }
}
