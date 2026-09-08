package org.sopt.buddys.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.verification.code.ExchangeVerificationErrorCode;
import org.sopt.buddys.domain.verification.entity.ExchangeVerification;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;
import org.sopt.buddys.domain.verification.repository.ExchangeVerificationRepository;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationSubmitResult;
import org.sopt.buddys.global.aws.s3.S3ObjectManager;
import org.sopt.buddys.global.aws.s3.S3ObjectMetadata;
import org.sopt.buddys.global.exception.BaseException;

class ExchangeVerificationServiceTest {

  private static final long USER_ID = 7L;
  private static final String DOCUMENT_KEY =
      "exchange-verifications/7/123e4567-e89b-12d3-a456-426614174000.pdf";
  private static final long FILE_SIZE = 823_044L;

  private ExchangeVerificationRepository exchangeVerificationRepository;
  private UserRepository userRepository;
  private S3ObjectManager s3ObjectManager;
  private ExchangeVerificationService service;

  @BeforeEach
  void setUp() {
    exchangeVerificationRepository = mock(ExchangeVerificationRepository.class);
    userRepository = mock(UserRepository.class);
    s3ObjectManager = mock(S3ObjectManager.class);
    service = new ExchangeVerificationService(
        exchangeVerificationRepository,
        userRepository,
        s3ObjectManager
    );
  }

  @DisplayName("S3 업로드 정보가 일치하면 대기 상태의 인증 신청을 저장한다")
  @Test
  void submit_validDocument_savesPendingVerification() {
    // given
    User user = mock(User.class);
    ExchangeVerification saved = mock(ExchangeVerification.class);
    given(saved.getId()).willReturn(12L);
    given(saved.getStatus()).willReturn(ExchangeVerificationStatus.PENDING);
    given(s3ObjectManager.findMetadata(DOCUMENT_KEY))
        .willReturn(Optional.of(new S3ObjectMetadata("application/pdf", FILE_SIZE)));
    given(userRepository.findByIdForProfileUpdate(USER_ID)).willReturn(Optional.of(user));
    given(exchangeVerificationRepository.save(any(ExchangeVerification.class))).willReturn(saved);

    // when
    ExchangeVerificationSubmitResult result = service.submit(
        USER_ID,
        DOCUMENT_KEY,
        "교환학생_입학허가서.pdf",
        "application/pdf",
        FILE_SIZE
    );

    // then
    assertThat(result.verificationId()).isEqualTo(12L);
    assertThat(result.status()).isEqualTo(ExchangeVerificationStatus.PENDING);
    ArgumentCaptor<ExchangeVerification> captor = ArgumentCaptor.forClass(
        ExchangeVerification.class
    );
    verify(exchangeVerificationRepository).save(captor.capture());
    assertThat(captor.getValue().getUser()).isSameAs(user);
    assertThat(captor.getValue().getDocumentKey()).isEqualTo(DOCUMENT_KEY);
    assertThat(captor.getValue().getOriginalFileName()).isEqualTo("교환학생_입학허가서.pdf");
    assertThat(captor.getValue().getContentType()).isEqualTo("application/pdf");
    assertThat(captor.getValue().getFileSize()).isEqualTo(FILE_SIZE);
    assertThat(captor.getValue().getStatus()).isEqualTo(ExchangeVerificationStatus.PENDING);
  }

  @DisplayName("다른 사용자의 documentKey로 신청할 수 없다")
  @Test
  void submit_otherUsersDocumentKey_throwsInvalidDocumentKey() {
    assertThatThrownBy(() -> service.submit(
        USER_ID,
        "exchange-verifications/8/123e4567-e89b-12d3-a456-426614174000.pdf",
        "document.pdf",
        "application/pdf",
        FILE_SIZE
    )).isInstanceOfSatisfying(BaseException.class, exception ->
        assertThat(exception.getErrorCode())
            .isEqualTo(ExchangeVerificationErrorCode.INVALID_DOCUMENT_KEY)
    );
    verifyNoInteractions(s3ObjectManager, userRepository, exchangeVerificationRepository);
  }

  @DisplayName("S3에 업로드된 객체가 없으면 신청을 접수하지 않는다")
  @Test
  void submit_missingS3Object_throwsDocumentNotUploaded() {
    given(s3ObjectManager.findMetadata(DOCUMENT_KEY)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.submit(
        USER_ID,
        DOCUMENT_KEY,
        "document.pdf",
        "application/pdf",
        FILE_SIZE
    )).isInstanceOfSatisfying(BaseException.class, exception ->
        assertThat(exception.getErrorCode())
            .isEqualTo(ExchangeVerificationErrorCode.DOCUMENT_NOT_UPLOADED)
    );
    verifyNoInteractions(userRepository, exchangeVerificationRepository);
  }

  @DisplayName("S3 객체의 Content-Type 또는 크기가 다르면 신청을 접수하지 않는다")
  @Test
  void submit_mismatchedS3Metadata_throwsMetadataMismatch() {
    given(s3ObjectManager.findMetadata(DOCUMENT_KEY))
        .willReturn(Optional.of(new S3ObjectMetadata("application/pdf", FILE_SIZE + 1)));

    assertThatThrownBy(() -> service.submit(
        USER_ID,
        DOCUMENT_KEY,
        "document.pdf",
        "application/pdf",
        FILE_SIZE
    )).isInstanceOfSatisfying(BaseException.class, exception ->
        assertThat(exception.getErrorCode())
            .isEqualTo(ExchangeVerificationErrorCode.DOCUMENT_METADATA_MISMATCH)
    );
    verifyNoInteractions(userRepository, exchangeVerificationRepository);
  }

  @DisplayName("이미 대기 중인 인증 신청이 있으면 중복 신청을 거부한다")
  @Test
  void submit_existingPendingVerification_throwsConflict() {
    given(s3ObjectManager.findMetadata(DOCUMENT_KEY))
        .willReturn(Optional.of(new S3ObjectMetadata("application/pdf", FILE_SIZE)));
    given(userRepository.findByIdForProfileUpdate(USER_ID))
        .willReturn(Optional.of(mock(User.class)));
    given(exchangeVerificationRepository.existsByUserIdAndStatus(
        USER_ID,
        ExchangeVerificationStatus.PENDING
    )).willReturn(true);

    assertThatThrownBy(() -> service.submit(
        USER_ID,
        DOCUMENT_KEY,
        "document.pdf",
        "application/pdf",
        FILE_SIZE
    )).isInstanceOfSatisfying(BaseException.class, exception ->
        assertThat(exception.getErrorCode())
            .isEqualTo(ExchangeVerificationErrorCode.PENDING_VERIFICATION_ALREADY_EXISTS)
    );
  }
}
