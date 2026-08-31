package org.sopt.buddys.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.location.entity.University;
import org.sopt.buddys.domain.location.repository.UniversityRepository;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.verification.code.UniversityVerificationErrorCode;
import org.sopt.buddys.domain.verification.config.UniversityVerificationProperties;
import org.sopt.buddys.domain.verification.entity.UniversityVerification;
import org.sopt.buddys.domain.verification.repository.UniversityVerificationRepository;
import org.sopt.buddys.domain.verification.repository.UniversityVerificationRepository.VerificationResult;
import org.sopt.buddys.global.exception.BaseException;

@ExtendWith(MockitoExtension.class)
class UniversityVerificationServiceTest {

  private static final long USER_ID = 1L;
  private static final long UNIVERSITY_ID = 10L;
  private static final String EMAIL = "student@university.ac.kr";
  private static final String UNIVERSITY_NAME = "Buddys University";
  private static final Duration CODE_EXPIRATION = Duration.ofMinutes(15);
  private static final int MAX_ATTEMPTS = 5;

  @Mock
  private UniversityVerificationRepository universityVerificationRepository;

  @Mock
  private UniversityRepository universityRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UniversityVerificationMailSender mailSender;

  @Mock
  private User user;

  @Mock
  private University university;

  private UniversityVerificationService universityVerificationService;

  @BeforeEach
  void setUp() {
    UniversityVerificationProperties properties = new UniversityVerificationProperties(
        CODE_EXPIRATION,
        MAX_ATTEMPTS
    );
    universityVerificationService = new UniversityVerificationService(
        universityVerificationRepository,
        universityRepository,
        userRepository,
        properties,
        mailSender
    );
  }

  @DisplayName("인증 요청은 6자리 코드를 15분 TTL로 저장한 뒤 코드가 담긴 메일을 발송한다")
  @Test
  void sendVerification_savesCodeWithTtlAndSendsMail() {
    // given
    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(user));
    given(universityRepository.findFirstByDomainIgnoreCase("university.ac.kr"))
        .willReturn(Optional.of(university));
    given(university.getId()).willReturn(UNIVERSITY_ID);
    given(university.getName()).willReturn(UNIVERSITY_NAME);

    // when
    universityVerificationService.sendVerification(USER_ID, EMAIL);

    // then
    ArgumentCaptor<UniversityVerification> verificationCaptor =
        ArgumentCaptor.forClass(UniversityVerification.class);
    then(universityVerificationRepository).should().save(
        verificationCaptor.capture(),
        eq(CODE_EXPIRATION)
    );

    UniversityVerification saved = verificationCaptor.getValue();
    assertThat(saved.userId()).isEqualTo(USER_ID);
    assertThat(saved.universityId()).isEqualTo(UNIVERSITY_ID);
    assertThat(saved.email()).isEqualTo(EMAIL);
    assertThat(saved.code()).matches("^[A-Z0-9]{6}$");
    then(mailSender).should().send(EMAIL, UNIVERSITY_NAME, saved.code());
  }

  @DisplayName("메일 발송에 실패하면 이번 요청으로 저장한 인증 코드를 제거한다")
  @Test
  void sendVerification_mailFailure_deletesSavedCode() {
    // given
    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(user));
    given(universityRepository.findFirstByDomainIgnoreCase("university.ac.kr"))
        .willReturn(Optional.of(university));
    given(university.getId()).willReturn(UNIVERSITY_ID);
    given(university.getName()).willReturn(UNIVERSITY_NAME);
    BaseException mailException = new BaseException(UniversityVerificationErrorCode.MAIL_SEND_FAILED);
    org.mockito.BDDMockito.willThrow(mailException)
        .given(mailSender).send(any(), any(), any());

    // when & then
    assertThatThrownBy(() -> universityVerificationService.sendVerification(USER_ID, EMAIL))
        .isSameAs(mailException);

    ArgumentCaptor<UniversityVerification> verificationCaptor =
        ArgumentCaptor.forClass(UniversityVerification.class);
    then(universityVerificationRepository).should().deleteIfMatches(verificationCaptor.capture());
    assertThat(verificationCaptor.getValue().userId()).isEqualTo(USER_ID);
  }

  @DisplayName("코드가 일치하면 사용자의 학교를 인증하고 코드를 한 번만 삭제한다")
  @Test
  void confirmVerification_validCode_verifiesUniversityAndDeletesCode() {
    // given
    UniversityVerification verification = UniversityVerification.issue(USER_ID, UNIVERSITY_ID, EMAIL);
    given(universityVerificationRepository.verifyCode(USER_ID, verification.code(), MAX_ATTEMPTS))
        .willReturn(VerificationResult.matched(verification));
    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(user));
    given(universityRepository.findById(UNIVERSITY_ID)).willReturn(Optional.of(university));

    // when
    universityVerificationService.confirmVerification(USER_ID, verification.code());

    // then
    then(user).should().verifyUniversity(university);
    then(universityVerificationRepository).should().deleteIfMatches(verification);
  }

  @DisplayName("코드가 없거나 틀리면 인증 코드 오류가 발생한다")
  @Test
  void confirmVerification_missingCode_throwsInvalid() {
    // given
    given(universityVerificationRepository.verifyCode(eq(USER_ID), any(), eq(MAX_ATTEMPTS)))
        .willReturn(VerificationResult.invalid());

    // when & then
    assertThatThrownBy(() -> universityVerificationService.confirmVerification(USER_ID, "ABC123"))
        .isInstanceOf(BaseException.class)
        .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
            .isEqualTo(UniversityVerificationErrorCode.VERIFICATION_CODE_INVALID));
    then(userRepository).should(never()).findByIdAndDeletedAtIsNull(any());
  }

  @DisplayName("인증 코드 입력 횟수를 초과하면 별도의 제한 초과 오류가 발생한다")
  @Test
  void confirmVerification_attemptLimitExceeded_throwsLimitExceeded() {
    // given
    given(universityVerificationRepository.verifyCode(eq(USER_ID), any(), eq(MAX_ATTEMPTS)))
        .willReturn(VerificationResult.limitExceeded());

    // when & then
    assertThatThrownBy(() -> universityVerificationService.confirmVerification(USER_ID, "ABC123"))
        .isInstanceOf(BaseException.class)
        .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
            .isEqualTo(UniversityVerificationErrorCode.VERIFICATION_ATTEMPT_LIMIT_EXCEEDED));
    then(userRepository).should(never()).findByIdAndDeletedAtIsNull(any());
  }
}
