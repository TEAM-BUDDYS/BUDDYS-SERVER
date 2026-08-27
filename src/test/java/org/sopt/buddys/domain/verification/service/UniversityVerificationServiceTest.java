package org.sopt.buddys.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.sopt.buddys.global.exception.BaseException;

@ExtendWith(MockitoExtension.class)
class UniversityVerificationServiceTest {

  private static final long USER_ID = 1L;
  private static final long UNIVERSITY_ID = 10L;
  private static final String EMAIL = "student@university.ac.kr";
  private static final String UNIVERSITY_NAME = "Buddys University";
  private static final Duration TOKEN_EXPIRATION = Duration.ofMinutes(15);

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
        TOKEN_EXPIRATION,
        "http://localhost:8080/api/v1/verifications/university/email/confirm"
    );
    universityVerificationService = new UniversityVerificationService(
        universityVerificationRepository,
        universityRepository,
        userRepository,
        properties,
        mailSender
    );
  }

  @DisplayName("학교 인증 요청은 사용자별 토큰을 15분 TTL로 저장한 뒤 메일을 발송한다")
  @Test
  void sendVerification_savesTokenWithTtlAndSendsMail() {
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
        org.mockito.ArgumentMatchers.eq(Duration.ofMinutes(15))
    );

    UniversityVerification saved = verificationCaptor.getValue();
    assertThat(saved.userId()).isEqualTo(USER_ID);
    assertThat(saved.universityId()).isEqualTo(UNIVERSITY_ID);
    assertThat(saved.email()).isEqualTo(EMAIL);
    assertThat(saved.token()).startsWith(USER_ID + ".");
    then(mailSender).should().send(EMAIL, UNIVERSITY_NAME, saved.token());
  }

  @DisplayName("메일 발송에 실패하면 이번 요청으로 저장한 인증 토큰을 제거한다")
  @Test
  void sendVerification_mailFailure_deletesSavedToken() {
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
    then(universityVerificationRepository).should().deleteIfTokenMatches(verificationCaptor.capture());
    assertThat(verificationCaptor.getValue().userId()).isEqualTo(USER_ID);
  }

  @DisplayName("유효한 인증 토큰이면 사용자의 학교를 인증하고 토큰을 한 번만 삭제한다")
  @Test
  void confirmVerification_validToken_verifiesUniversityAndDeletesToken() {
    // given
    UniversityVerification verification = UniversityVerification.issue(USER_ID, UNIVERSITY_ID, EMAIL);
    given(universityVerificationRepository.findByToken(verification.token()))
        .willReturn(Optional.of(verification));
    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(user));
    given(universityRepository.findById(UNIVERSITY_ID)).willReturn(Optional.of(university));

    // when
    universityVerificationService.confirmVerification(verification.token());

    // then
    then(user).should().verifyUniversity(university);
    then(universityVerificationRepository).should().deleteIfTokenMatches(verification);
  }

  @DisplayName("Redis에 토큰이 없으면 유효하지 않은 인증 링크 오류가 발생한다")
  @Test
  void confirmVerification_missingToken_throwsNotFound() {
    // given
    String token = "1.invalid-token";
    given(universityVerificationRepository.findByToken(token)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> universityVerificationService.confirmVerification(token))
        .isInstanceOf(BaseException.class)
        .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
            .isEqualTo(UniversityVerificationErrorCode.VERIFICATION_TOKEN_NOT_FOUND));
    then(userRepository).should(never()).findByIdAndDeletedAtIsNull(any());
  }
}
