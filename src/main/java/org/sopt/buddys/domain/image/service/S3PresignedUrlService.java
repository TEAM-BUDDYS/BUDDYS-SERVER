package org.sopt.buddys.domain.image.service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.domain.image.code.ImageErrorCode;
import org.sopt.buddys.domain.image.entity.ImageDomain;
import org.sopt.buddys.domain.image.entity.SupportedImageType;
import org.sopt.buddys.global.aws.s3.S3PresignedUploadResult;
import org.sopt.buddys.global.aws.s3.S3PresignedUrlManager;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {

  private final S3PresignedUrlManager s3PresignedUrlManager;

  public S3PresignedUploadResult createUploadUrl(Long userId, ImageDomain imageDomain, String contentType) {
    SupportedImageType imageType = SupportedImageType.fromContentType(contentType)
        .orElseThrow(() -> new BaseException(ImageErrorCode.UNSUPPORTED_CONTENT_TYPE));

    String key = imageDomain.getFolder() + "/" + UUID.randomUUID() + imageType.getExtension();
    log.info("[Image] presigned-url 발급 userId={}, domain={}, key={}", userId, imageDomain, key);

    return s3PresignedUrlManager.createUploadUrl(key, imageType.getContentType());
  }
}