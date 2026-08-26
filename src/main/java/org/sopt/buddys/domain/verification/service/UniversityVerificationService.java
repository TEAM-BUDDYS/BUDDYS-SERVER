package org.sopt.buddys.domain.verification.service;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversityVerificationService {

  private final UniversityVerificationRepository universityVerificationRepository;
  private final UniversityRepository universityRepository;
  private final UserRepository userRepository;
  private final UniversityVerificationProperties universityVerificationProperties;
  private final UniversityVerificationMailSender mailSender;
현
  @Transactional
  public void sendVerification(Long userId, String email) {
    userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

    University university = resolveUniversityByEmail(email);

    UniversityVerification verification = UniversityVerification.of(
        userId, university.getId(), email, universityVerificationProperties.tokenExpiration()
    );
    universityVerificationRepository.deleteById(userId);
    universityVerificationRepository.save(verification);

    mailSender.send(email, university.getName(), verification.getToken());
  }

  @Transactional
  public void confirmVerification(String token) {
    UniversityVerification verification = universityVerificationRepository.findByToken(token)
        .orElseThrow(() -> new BaseException(UniversityVerificationErrorCode.VERIFICATION_TOKEN_NOT_FOUND));

    if (verification.isExpired()) {
      throw new BaseException(UniversityVerificationErrorCode.VERIFICATION_TOKEN_EXPIRED);
    }

    User user = userRepository.findByIdAndDeletedAtIsNull(verification.getUserId())
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
    University university = universityRepository.findById(verification.getUniversityId())
        .orElseThrow(() -> new BaseException(LocationErrorCode.UNIVERSITY_NOT_FOUND));

    user.verifyUniversity(university);
    universityVerificationRepository.delete(verification);
  }

  private University resolveUniversityByEmail(String email) {
    String emailDomain = email.substring(email.indexOf('@') + 1);
    return universityRepository.findFirstByDomainIgnoreCase(emailDomain)
        .orElseThrow(() -> new BaseException(LocationErrorCode.UNIVERSITY_NOT_FOUND));
  }
}
