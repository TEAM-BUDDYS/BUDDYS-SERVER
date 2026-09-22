package org.sopt.buddys.global.aws.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class S3PresignedPostPolicyTest {

  private static final long ONE_MIB = 1024L * 1024;
  private static final String KEY = "exchange-verifications/7/document.pdf";
  private static final String BUCKET = "buddys-test-bucket";

  @DisplayName("POST 정책은 키와 Content-Type을 고정하고 파일 크기를 발급 크기로 제한한다")
  @Test
  void createPostUpload_includesSignedContentLengthRange() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    S3PresignedUrlManager manager = manager(
        () -> AwsBasicCredentials.create("test-access-key", "test-secret-key"),
        objectMapper
    );

    S3PresignedPostResult result = manager.createPostUpload(KEY, "application/pdf", ONE_MIB);

    assertThat(result.uploadUrl())
        .isEqualTo("https://buddys-test-bucket.s3.ap-northeast-2.amazonaws.com/");
    assertThat(result.fields()).containsEntry("key", KEY)
        .containsEntry("Content-Type", "application/pdf")
        .containsEntry("success_action_status", "204")
        .containsKeys("policy", "x-amz-signature", "x-amz-credential", "x-amz-date");

    JsonNode policy = decodePolicy(result.fields(), objectMapper);
    assertThat(policy.get("conditions").toString())
        .contains("[\"content-length-range\",1,1048576]")
        .contains("\"bucket\":\"" + BUCKET + "\"")
        .contains("\"key\":\"" + KEY + "\"")
        .contains("\"Content-Type\":\"application/pdf\"");
    assertThat(result.fields().get("x-amz-signature")).matches("[0-9a-f]{64}");
    assertThat(result.fields().toString()).doesNotContain("test-secret-key");
  }

  @DisplayName("EC2 역할 같은 임시 자격 증명은 토큰을 POST 폼과 정책에 포함한다")
  @Test
  void createPostUpload_withSessionCredentials_includesToken() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    S3PresignedUrlManager manager = manager(
        () -> AwsSessionCredentials.create("test-access-key", "test-secret-key", "test-token"),
        objectMapper
    );

    S3PresignedPostResult result = manager.createPostUpload(KEY, "application/pdf", ONE_MIB);

    assertThat(result.fields()).containsEntry("x-amz-security-token", "test-token");
    assertThat(decodePolicy(result.fields(), objectMapper).get("conditions").toString())
        .contains("\"x-amz-security-token\":\"test-token\"");
  }

  private JsonNode decodePolicy(Map<String, String> fields, ObjectMapper objectMapper)
      throws Exception {
    byte[] json = Base64.getDecoder().decode(fields.get("policy"));
    return objectMapper.readTree(new String(json, StandardCharsets.UTF_8));
  }

  private S3PresignedUrlManager manager(
      AwsCredentialsProvider credentialsProvider,
      ObjectMapper objectMapper
  ) throws Exception {
    S3Utilities utilities = mock(S3Utilities.class);
    S3Properties properties = mock(S3Properties.class);
    when(properties.getBucket()).thenReturn(BUCKET);
    when(properties.getRegion()).thenReturn("ap-northeast-2");
    when(utilities.getUrl(any(GetUrlRequest.class))).thenReturn(new URL(
        "https://buddys-test-bucket.s3.ap-northeast-2.amazonaws.com/" + KEY
    ));
    return new S3PresignedUrlManager(
        mock(S3Presigner.class), utilities, properties, credentialsProvider, objectMapper
    );
  }
}
