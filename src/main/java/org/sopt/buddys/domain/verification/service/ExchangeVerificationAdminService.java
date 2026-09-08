package org.sopt.buddys.domain.verification.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.verification.entity.ExchangeVerification;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;
import org.sopt.buddys.domain.verification.repository.ExchangeVerificationRepository;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationListResult;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationListResult.ExchangeVerificationSummaryResult;
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
        ? exchangeVerificationRepository.findLatestByUser(pageable)
        : exchangeVerificationRepository.findLatestByUserAndStatus(status, pageable);

    return new ExchangeVerificationListResult(
        verifications.getContent().stream()
            .map(ExchangeVerificationSummaryResult::from)
            .toList(),
        verifications.getNumber(),
        verifications.getSize(),
        verifications.hasNext()
    );
  }

  private void validateAdmin(Long userId) {
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(GlobalErrorCode.UNAUTHORIZED));
    if (!user.isAdmin()) {
      throw new BaseException(GlobalErrorCode.FORBIDDEN);
    }
  }
}
