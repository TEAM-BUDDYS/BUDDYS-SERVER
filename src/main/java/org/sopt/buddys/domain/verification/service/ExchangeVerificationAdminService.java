package org.sopt.buddys.domain.verification.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.verification.code.ExchangeVerificationErrorCode;
import org.sopt.buddys.domain.verification.entity.ExchangeVerification;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;
import org.sopt.buddys.domain.verification.repository.ExchangeVerificationRepository;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationDetailResult;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationListResult;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationListResult.ExchangeVerificationSummaryResult;
import org.sopt.buddys.global.aws.s3.S3PresignedUrlManager;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExchangeVerificationAdminService {

  private final ExchangeVerificationRepository exchangeVerificationRepository;
  private final UserRepository userRepository;
  private final S3PresignedUrlManager s3PresignedUrlManager;

  @Transactional(readOnly = true)
  public ExchangeVerificationListResult getVerifications(
      Long adminUserId,
      ExchangeVerificationStatus status,
      int page,
      int size
  ) {
    validateAdmin(adminUserId);

    Pageable pageable = PageRequest.of(page, size);
    Slice<ExchangeVerification> verifications = status == null
        ? exchangeVerificationRepository.findAllWithUser(pageable)
        : exchangeVerificationRepository.findAllWithUserByStatus(status, pageable);

    return new ExchangeVerificationListResult(
        verifications.getContent().stream()
            .map(ExchangeVerificationSummaryResult::from)
            .toList(),
        verifications.getNumber(),
        verifications.getSize(),
        verifications.hasNext()
    );
  }

  @Transactional(readOnly = true)
  public ExchangeVerificationDetailResult getVerification(Long adminUserId, Long verificationId) {
    validateAdmin(adminUserId);

    ExchangeVerification verification = exchangeVerificationRepository
        .findByIdWithUser(verificationId)
        .orElseThrow(() -> new BaseException(
            ExchangeVerificationErrorCode.VERIFICATION_NOT_FOUND
        ));
    String documentUrl = s3PresignedUrlManager.createGetUrl(verification.getDocumentKey());

    return ExchangeVerificationDetailResult.of(verification, documentUrl);
  }

  @Transactional
  public void approveVerification(Long adminUserId, Long verificationId) {
    User reviewer = validateAdmin(adminUserId);
    ExchangeVerification verification = findPendingVerificationForUpdate(verificationId);

    verification.approve(reviewer);
    verification.getUser().verifyExchange();
  }

  @Transactional
  public void rejectVerification(
      Long adminUserId,
      Long verificationId,
      String rejectionReason
  ) {
    User reviewer = validateAdmin(adminUserId);
    ExchangeVerification verification = findPendingVerificationForUpdate(verificationId);

    verification.reject(reviewer, rejectionReason.trim());
  }

  private ExchangeVerification findPendingVerificationForUpdate(Long verificationId) {
    ExchangeVerification verification = exchangeVerificationRepository
        .findByIdWithUserForUpdate(verificationId)
        .orElseThrow(() -> new BaseException(
            ExchangeVerificationErrorCode.VERIFICATION_NOT_FOUND
        ));
    if (!verification.isPending()) {
      throw new BaseException(ExchangeVerificationErrorCode.VERIFICATION_ALREADY_REVIEWED);
    }
    return verification;
  }

  private User validateAdmin(Long userId) {
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(GlobalErrorCode.UNAUTHORIZED));
    if (!user.isAdmin()) {
      throw new BaseException(GlobalErrorCode.FORBIDDEN);
    }
    return user;
  }
}
