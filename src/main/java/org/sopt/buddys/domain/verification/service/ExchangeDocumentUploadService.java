package org.sopt.buddys.domain.verification.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.domain.verification.code.ExchangeVerificationErrorCode;
import org.sopt.buddys.domain.verification.entity.SupportedExchangeDocumentType;
import org.sopt.buddys.domain.verification.service.result.ExchangeDocumentUploadUrlResult;
import org.sopt.buddys.global.aws.s3.S3PresignedUrlManager;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeDocumentUploadService {

  private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
  private static final String DOCUMENT_FOLDER = "exchange-verifications";

  private final S3PresignedUrlManager s3PresignedUrlManager;

  public ExchangeDocumentUploadUrlResult createUploadUrl(
      Long userId,
      String contentType,
      Long fileSize
  ) {
    validateFileSize(fileSize);

    SupportedExchangeDocumentType documentType =
        SupportedExchangeDocumentType.fromContentType(contentType)
            .orElseThrow(() -> new BaseException(
                ExchangeVerificationErrorCode.UNSUPPORTED_DOCUMENT_TYPE
            ));

    String documentKey = DOCUMENT_FOLDER
        + "/" + userId
        + "/" + UUID.randomUUID()
        + documentType.getExtension();

    String uploadUrl = s3PresignedUrlManager.createPutUrl(
        documentKey,
        documentType.getContentType(),
        fileSize
    );

    log.info(
        "[ExchangeVerification] document presigned-url issued userId={}, contentType={}, fileSize={}",
        userId,
        documentType.getContentType(),
        fileSize
    );

    return new ExchangeDocumentUploadUrlResult(uploadUrl, documentKey);
  }

  private void validateFileSize(Long fileSize) {
    if (fileSize == null || fileSize <= 0 || fileSize > MAX_FILE_SIZE_BYTES) {
      throw new BaseException(ExchangeVerificationErrorCode.DOCUMENT_FILE_TOO_LARGE);
    }
  }
}
