package org.sopt.buddys.domain.verification.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.location.code.LocationErrorCode;
import org.sopt.buddys.domain.location.entity.University;
import org.sopt.buddys.domain.location.repository.UniversityRepository;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.verification.config.UniversityVerificationProperties;
import org.sopt.buddys.domain.verification.entity.UniversityVerification;
import org.sopt.buddys.domain.verification.repository.UniversityVerificationRepository;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UniversityVerificationIssueService {

  private final UserRepository userRepository;
  private final UniversityRepository universityRepository;
  private final UniversityVerificationRepository universityVerificationRepository;
  private final UniversityVerificationProperties universityVerificationProperties;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public IssuedVerification issue(Long userId, String email) {
    userRepository.findActiveByIdForUpdate(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
    String emailDomain = email.substring(email.indexOf('@') + 1);
    University university = universityRepository.findFirstByDomainIgnoreCase(emailDomain)
        .orElseThrow(() -> new BaseException(LocationErrorCode.UNIVERSITY_NOT_FOUND));
    UniversityVerification verification = UniversityVerification.issue(userId, university.getId(), email);
    universityVerificationRepository.save(verification, universityVerificationProperties.codeExpiration());
    return new IssuedVerification(verification, university.getName());
  }

  public record IssuedVerification(UniversityVerification verification, String universityName) {
  }
}
