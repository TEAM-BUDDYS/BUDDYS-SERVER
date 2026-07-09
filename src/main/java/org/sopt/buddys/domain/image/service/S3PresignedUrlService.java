package org.sopt.buddys.domain.image.service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.domain.image.code.ImageErrorCode;
import org.sopt.buddys.domain.image.entity.ImageDomain;
import org.sopt.buddys.global.aws.s3.S3PresignedUploadResult;
import org.sopt.buddys.global.aws.s3.S3PresignedUrlManager;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {

  private static final Map<String, String> SUPPORTED_CONTENT_TYPE_EXTENSIONS = Map.of(
      "image/jpeg", ".jpg",
      "image/png", ".png",
      "image/webp", ".webp"
  );

  private final S3PresignedUrlManager s3PresignedUrlManager;

  public S3PresignedUploadResult createUploadUrl(Long userId, ImageDomain imageDomain, String contentType) {
    String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
    String extension = resolveExtension(normalizedContentType);
    String key = imageDomain.getFolder() + "/" + UUID.randomUUID() + extension;

    log.info("[Image] presigned-url 발급 userId={}, domain={}, key={}", userId, imageDomain, key);

    return s3PresignedUrlManager.createUploadUrl(key, normalizedContentType);
  }

  private String resolveExtension(String contentType) {
    return Optional.ofNullable(SUPPORTED_CONTENT_TYPE_EXTENSIONS.get(contentType))
        .orElseThrow(() -> new BaseException(ImageErrorCode.UNSUPPORTED_CONTENT_TYPE));
  }
}