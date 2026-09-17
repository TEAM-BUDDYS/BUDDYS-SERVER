package org.sopt.buddys.global.aws.s3;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class S3PresignedUrlManager {

  private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofMinutes(5);
  private static final DateTimeFormatter AMZ_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
  private static final String SIGNING_ALGORITHM = "AWS4-HMAC-SHA256";

  private final S3Presigner s3Presigner;
  private final S3Utilities s3Utilities;
  private final S3Properties s3Properties;
  private final AwsCredentialsProvider awsCredentialsProvider;
  private final ObjectMapper objectMapper;

  public S3PresignedUploadResult createUploadUrl(String key, String contentType, long fileSize) {
    S3PresignedPostResult post = createPostUpload(key, contentType, fileSize);
    return new S3PresignedUploadResult(
        post.uploadUrl(),
        post.fields(),
        buildPublicUrl(key)
    );
  }

  public S3PresignedPostResult createPostUpload(String key, String contentType, long fileSize) {
    if (fileSize <= 0) {
      throw new IllegalArgumentException("fileSize must be positive");
    }

    AwsCredentials credentials = awsCredentialsProvider.resolveCredentials();
    Instant now = Instant.now();
    String date = LocalDate.ofInstant(now, ZoneOffset.UTC)
        .format(DateTimeFormatter.BASIC_ISO_DATE);
    String amzDate = AMZ_DATE_FORMAT.format(now);
    String credential = credentials.accessKeyId() + "/" + date + "/"
        + s3Properties.getRegion() + "/s3/aws4_request";

    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("key", key);
    fields.put("Content-Type", contentType);
    fields.put("success_action_status", "204");
    fields.put("x-amz-algorithm", SIGNING_ALGORITHM);
    fields.put("x-amz-credential", credential);
    fields.put("x-amz-date", amzDate);
    if (credentials instanceof AwsSessionCredentials sessionCredentials) {
      fields.put("x-amz-security-token", sessionCredentials.sessionToken());
    }

    List<Object> conditions = new ArrayList<>();
    conditions.add(Map.of("bucket", s3Properties.getBucket()));
    fields.forEach((name, value) -> conditions.add(Map.of(name, value)));
    conditions.add(List.of("content-length-range", 1, fileSize));

    String policy = encodePolicy(now.plus(PRESIGNED_URL_EXPIRATION), conditions);
    fields.put("policy", policy);
    fields.put("x-amz-signature", signPolicy(policy, credentials.secretAccessKey(), date));

    URL objectUrl = s3Utilities.getUrl(GetUrlRequest.builder()
        .bucket(s3Properties.getBucket())
        .key(key)
        .build());
    String bucketUrl = objectUrl.getProtocol() + "://" + objectUrl.getAuthority() + "/";
    return new S3PresignedPostResult(bucketUrl, fields);
  }

  public String createGetUrl(String key) {
    GetObjectRequest objectRequest = GetObjectRequest.builder()
        .bucket(s3Properties.getBucket())
        .key(key)
        .build();

    PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(
        GetObjectPresignRequest.builder()
            .signatureDuration(PRESIGNED_URL_EXPIRATION)
            .getObjectRequest(objectRequest)
            .build()
    );

    return presigned.url().toString();
  }

  private String buildPublicUrl(String key) {
    return s3Utilities.getUrl(GetUrlRequest.builder()
            .bucket(s3Properties.getBucket())
            .key(key)
            .build())
        .toString();
  }

  private String encodePolicy(Instant expiration, List<Object> conditions) {
    try {
      String json = objectMapper.writeValueAsString(Map.of(
          "expiration", expiration.toString(),
          "conditions", conditions
      ));
      return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    } catch (JacksonException exception) {
      throw new IllegalStateException("Could not serialize S3 POST policy", exception);
    }
  }

  private String signPolicy(String policy, String secretKey, String date) {
    byte[] signingKey = hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), date);
    signingKey = hmac(signingKey, s3Properties.getRegion());
    signingKey = hmac(signingKey, "s3");
    signingKey = hmac(signingKey, "aws4_request");
    return HexFormat.of().formatHex(hmac(signingKey, policy));
  }

  private byte[] hmac(byte[] key, String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    } catch (java.security.GeneralSecurityException exception) {
      throw new IllegalStateException("Could not sign S3 POST policy", exception);
    }
  }
}
