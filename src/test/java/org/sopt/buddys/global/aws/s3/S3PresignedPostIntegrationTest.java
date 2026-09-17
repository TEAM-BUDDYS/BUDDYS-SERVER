package org.sopt.buddys.global.aws.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import tools.jackson.databind.ObjectMapper;

/** 실제 S3 버킷에 테스트 전용 객체를 업로드하고 종료 시 해당 객체만 삭제한다. */
class S3PresignedPostIntegrationTest {

  private static final long ONE_MIB = 1024L * 1024;
  private static final int OVERSIZED_FILE_BYTES = 11 * 1024 * 1024;

  @DisplayName("1MiB 제한 POST 정책으로 10MiB 초과 파일을 올리면 S3가 저장 전에 거부한다")
  @Test
  @EnabledIfEnvironmentVariable(named = "BUDDYS_S3_POST_TEST_BUCKET", matches = ".+")
  @EnabledIfEnvironmentVariable(named = "BUDDYS_S3_POST_TEST_REGION", matches = ".+")
  void oversizedUpload_isRejectedBeforeObjectIsStored() throws Exception {
    String bucket = System.getenv("BUDDYS_S3_POST_TEST_BUCKET");
    Region region = Region.of(System.getenv("BUDDYS_S3_POST_TEST_REGION"));
    AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
    S3Properties properties = mock(S3Properties.class);
    when(properties.getBucket()).thenReturn(bucket);
    when(properties.getRegion()).thenReturn(region.id());

    S3Utilities utilities = S3Utilities.builder().region(region).build();
    S3PresignedUrlManager manager = new S3PresignedUrlManager(
        mock(S3Presigner.class), utilities, properties, credentialsProvider, new ObjectMapper()
    );
    String testPrefix = "exchange-verifications/7/s3-post-integration-" + UUID.randomUUID();
    String controlKey = testPrefix + "-control.pdf";
    String oversizedKey = testPrefix + "-oversized.pdf";

    try (S3Client s3 = S3Client.builder()
        .region(region)
        .credentialsProvider(credentialsProvider)
        .build()) {
      try {
        // 유효한 작은 업로드가 성공해야 크기 초과 거부가 권한/서명 오류 때문이 아님을 확인할 수 있다.
        S3PresignedPostResult control = manager.createPostUpload(
            controlKey, "application/pdf", ONE_MIB
        );
        HttpResponse<String> controlResponse = upload(control, new byte[(int) ONE_MIB]);
        assertThat(controlResponse.statusCode()).isEqualTo(204);
        assertThat(s3.headObject(HeadObjectRequest.builder()
            .bucket(bucket).key(controlKey).build()).contentLength()).isEqualTo(ONE_MIB);

        S3PresignedPostResult oversized = manager.createPostUpload(
            oversizedKey, "application/pdf", ONE_MIB
        );
        HttpResponse<String> oversizedResponse = upload(
            oversized, new byte[OVERSIZED_FILE_BYTES]
        );
        assertThat(oversizedResponse.statusCode()).isBetween(400, 499);
        assertThatThrownBy(() -> s3.headObject(HeadObjectRequest.builder()
            .bucket(bucket).key(oversizedKey).build()))
            .isInstanceOfSatisfying(S3Exception.class, exception ->
                assertThat(exception.statusCode()).isEqualTo(404)
            );
      } finally {
        // 테스트가 실패해도 이 테스트에서 만든 두 객체만 정리한다.
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(controlKey).build());
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(oversizedKey).build());
      }
    }
  }

  private HttpResponse<String> upload(S3PresignedPostResult post, byte[] file)
      throws Exception {
    String boundary = "buddys-post-test-" + UUID.randomUUID();
    List<byte[]> body = new ArrayList<>();
    for (Map.Entry<String, String> field : post.fields().entrySet()) {
      body.add(("--" + boundary + "\r\n"
          + "Content-Disposition: form-data; name=\"" + field.getKey() + "\"\r\n\r\n"
          + field.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
    }
    body.add(("--" + boundary + "\r\n"
        + "Content-Disposition: form-data; name=\"file\"; filename=\"document.pdf\"\r\n"
        + "Content-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
    body.add(file);
    body.add(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

    HttpRequest request = HttpRequest.newBuilder(URI.create(post.uploadUrl()))
        .timeout(Duration.ofSeconds(60))
        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .POST(HttpRequest.BodyPublishers.ofByteArrays(body))
        .build();
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
  }
}
