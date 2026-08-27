package org.sopt.buddys.domain.verification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.domain.location.code.LocationErrorCode;
import org.sopt.buddys.domain.location.entity.University;
import org.sopt.buddys.domain.location.repository.UniversityRepository;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.verification.code.UniversityVerificationErrorCode;
import org.sopt.buddys.domain.verification.config.UniversityVerificationProperties;
import org.sopt.buddys.domain.verification.entity.UniversityVerification;
import org.sopt.buddys.domain.verification.repository.UniversityVerificationRepository;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversityVerificationService {

  private final UniversityVerificationRepository universityVerificationRepository;
  private final UniversityRepository universityRepository;
  private final UserRepository userRepository;
  private final UniversityVerificationProperties universityVerificationProperties;
  private final UniversityVerificationMailSender mailSender;

  @Transactional
  public void sendVerification(Long userId, String email) {
    userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

    University university = resolveUniversityByEmail(email);

    UniversityVerification verification = UniversityVerification.issue(userId, university.getId(), email);
    universityVerificationRepository.save(
        verification,
        universityVerificationProperties.tokenExpiration()
    );

    try {
      mailSender.send(email, university.getName(), verification.token());
    } catch (RuntimeException e) {
      deleteAfterMailFailure(verification, e);
      throw e;
    }
  }

  @Transactional
  public void confirmVerification(String token) {
    UniversityVerification verification = universityVerificationRepository.findByToken(token)
        .orElseThrow(() -> new BaseException(UniversityVerificationErrorCode.VERIFICATION_TOKEN_NOT_FOUND));

    User user = userRepository.findByIdAndDeletedAtIsNull(verification.userId())
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
    University university = universityRepository.findById(verification.universityId())
        .orElseThrow(() -> new BaseException(LocationErrorCode.UNIVERSITY_NOT_FOUND));

    user.verifyUniversity(university);
    deleteAfterCommit(verification);
  }

  private University resolveUniversityByEmail(String email) {
    String emailDomain = email.substring(email.indexOf('@') + 1);
    return universityRepository.findFirstByDomainIgnoreCase(emailDomain)
        .orElseThrow(() -> new BaseException(LocationErrorCode.UNIVERSITY_NOT_FOUND));
  }

  private void deleteAfterMailFailure(UniversityVerification verification, RuntimeException cause) {
    try {
      universityVerificationRepository.deleteIfTokenMatches(verification);
    } catch (RuntimeException cleanupException) {
      cause.addSuppressed(cleanupException);
      log.error("Failed to clean up university verification token after mail failure", cleanupException);
    }
  }

  private void deleteAfterCommit(UniversityVerification verification) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      universityVerificationRepository.deleteIfTokenMatches(verification);
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        try {
          universityVerificationRepository.deleteIfTokenMatches(verification);
        } catch (RuntimeException e) {
          log.error("Failed to delete consumed university verification token", e);
        }
      }
    });
  }
}
