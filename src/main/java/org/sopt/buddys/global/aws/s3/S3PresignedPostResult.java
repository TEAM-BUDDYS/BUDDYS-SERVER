package org.sopt.buddys.global.aws.s3;

import java.util.Map;

public record S3PresignedPostResult(
    String uploadUrl,
    Map<String, String> fields
) {
  public S3PresignedPostResult {
    fields = Map.copyOf(fields);
  }
}
