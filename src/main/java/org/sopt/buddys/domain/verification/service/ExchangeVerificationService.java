package org.sopt.buddys.domain.verification.service;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.verification.code.ExchangeVerificationErrorCode;
import org.sopt.buddys.domain.verification.entity.ExchangeVerification;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;
import org.sopt.buddys.domain.verification.entity.SupportedExchangeDocumentType;
import org.sopt.buddys.domain.verification.repository.ExchangeVerificationRepository;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationSubmitResult;
import org.sopt.buddys.global.aws.s3.S3ObjectManager;
import org.sopt.buddys.global.aws.s3.S3ObjectMetadata;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeVerificationService {

  private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
  private static final Pattern DOCUMENT_KEY_PATTERN = Pattern.compile(
      "^exchange-verifications/(\\d+)/"
          + "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
          + "(\\.pdf|\\.jpg|\\.png)$"
  );

  private final ExchangeVerificationRepository exchangeVerificationRepository;
  private final UserRepository userRepository;
  private final S3ObjectManager s3ObjectManager;

  @Transactional
  public ExchangeVerificationSubmitResult submit(
      Long userId,
      String documentKey,
      String originalFileName,
      String contentType,
      Long fileSize
  ) {
    SupportedExchangeDocumentType documentType = validateDocument(
        userId, documentKey, contentType, fileSize
    );
    validateUploadedObject(documentKey, documentType.getContentType(), fileSize);

    User user = userRepository.findByIdForProfileUpdate(userId)
        .orElseThrow(() -> new BaseException(GlobalErrorCode.UNAUTHORIZED));

    Optional<ExchangeVerification> existingVerification = exchangeVerificationRepository
        .findByUserIdAndStatus(userId, ExchangeVerificationStatus.PENDING);

    ExchangeVerification verification;
    if (existingVerification.isPresent()) {
      verification = existingVerification.get();
      String previousDocumentKey = verification.getDocumentKey();
      verification.replaceDocument(
          documentKey,
          originalFileName,
          documentType.getContentType(),
          fileSize
      );
      deletePreviousDocumentAfterCommit(previousDocumentKey, documentKey);
    } else {
      verification = exchangeVerificationRepository.save(new ExchangeVerification(
          user,
          documentKey,
          originalFileName,
          documentType.getContentType(),
          fileSize
      ));
    }

    log.info(
        "[ExchangeVerification] submitted verificationId={}, userId={}, documentKey={}",
        verification.getId(),
        userId,
        documentKey
    );
    return new ExchangeVerificationSubmitResult(verification.getId(), verification.getStatus());
  }

  private SupportedExchangeDocumentType validateDocument(
      Long userId,
      String documentKey,
      String contentType,
      Long fileSize
  ) {
    if (fileSize == null || fileSize <= 0 || fileSize > MAX_FILE_SIZE_BYTES) {
      throw new BaseException(ExchangeVerificationErrorCode.DOCUMENT_FILE_TOO_LARGE);
    }

    SupportedExchangeDocumentType documentType =
        SupportedExchangeDocumentType.fromContentType(contentType)
            .orElseThrow(() -> new BaseException(
                ExchangeVerificationErrorCode.UNSUPPORTED_DOCUMENT_TYPE
            ));

    Matcher matcher = DOCUMENT_KEY_PATTERN.matcher(documentKey == null ? "" : documentKey);
    if (!matcher.matches()
        || !matcher.group(1).equals(String.valueOf(userId))
        || !matcher.group(2).equals(documentType.getExtension())) {
      throw new BaseException(ExchangeVerificationErrorCode.INVALID_DOCUMENT_KEY);
    }
    return documentType;
  }

  private void validateUploadedObject(String key, String contentType, long fileSize) {
    S3ObjectMetadata metadata = s3ObjectManager.findMetadata(key)
        .orElseThrow(() -> new BaseException(
            ExchangeVerificationErrorCode.DOCUMENT_NOT_UPLOADED
        ));

    String uploadedContentType = metadata.contentType() == null
        ? ""
        : metadata.contentType().toLowerCase(Locale.ROOT);
    if (!uploadedContentType.equals(contentType) || metadata.contentLength() != fileSize) {
      throw new BaseException(ExchangeVerificationErrorCode.DOCUMENT_METADATA_MISMATCH);
    }
  }

  private void deletePreviousDocumentAfterCommit(String previousKey, String newKey) {
    if (previousKey == null || previousKey.equals(newKey)) {
      return;
    }
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      deletePreviousDocument(previousKey);
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        deletePreviousDocument(previousKey);
      }
    });
  }

  private void deletePreviousDocument(String key) {
    try {
      s3ObjectManager.delete(key);
      log.info("[ExchangeVerification] previous document deleted documentKey={}", key);
    } catch (RuntimeException exception) {
      log.error("[ExchangeVerification] failed to delete previous document documentKey={}", key, exception);
    }
  }
}
