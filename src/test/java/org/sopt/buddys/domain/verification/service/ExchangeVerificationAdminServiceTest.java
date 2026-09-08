package org.sopt.buddys.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.verification.code.ExchangeVerificationErrorCode;
import org.sopt.buddys.domain.verification.entity.ExchangeVerification;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;
import org.sopt.buddys.domain.verification.repository.ExchangeVerificationRepository;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationDetailResult;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationListResult;
import org.sopt.buddys.global.aws.s3.S3PresignedUrlManager;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

class ExchangeVerificationAdminServiceTest {

  private static final long ADMIN_USER_ID = 1L;
  private static final long APPLICANT_USER_ID = 2L;

  private ExchangeVerificationRepository exchangeVerificationRepository;
  private UserRepository userRepository;
  private S3PresignedUrlManager s3PresignedUrlManager;
  private ExchangeVerificationAdminService service;

  @BeforeEach
  void setUp() {
    exchangeVerificationRepository = mock(ExchangeVerificationRepository.class);
    userRepository = mock(UserRepository.class);
    s3PresignedUrlManager = mock(S3PresignedUrlManager.class);
    service = new ExchangeVerificationAdminService(
        exchangeVerificationRepository,
        userRepository,
        s3PresignedUrlManager
    );
  }

  @DisplayName("관리자는 모든 서류 인증 신청을 최신순 페이지로 조회한다")
  @Test
  void getVerifications_withoutStatus_returnsAll() {
    // given
    User admin = mock(User.class);
    given(admin.isAdmin()).willReturn(true);
    given(userRepository.findByIdAndDeletedAtIsNull(ADMIN_USER_ID)).willReturn(Optional.of(admin));

    LocalDateTime submittedAt = LocalDateTime.of(2026, 8, 30, 14, 20);
    ExchangeVerification verification = verification(submittedAt, ExchangeVerificationStatus.PENDING);
    PageRequest pageable = PageRequest.of(0, 20);
    given(exchangeVerificationRepository.findAllWithUser(pageable))
        .willReturn(new SliceImpl<>(List.of(verification), pageable, false));

    // when
    ExchangeVerificationListResult result = service.getVerifications(ADMIN_USER_ID, null, 0, 20);

    // then
    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).verificationId()).isEqualTo(10L);
    assertThat(result.content().get(0).userId()).isEqualTo(APPLICANT_USER_ID);
    assertThat(result.content().get(0).nickname()).isEqualTo("지현");
    assertThat(result.content().get(0).submittedAt()).isEqualTo(submittedAt);
    assertThat(result.content().get(0).status()).isEqualTo(ExchangeVerificationStatus.PENDING);
    assertThat(result.hasNext()).isFalse();
  }

  @DisplayName("상태를 전달하면 해당 상태의 서류 인증 신청만 조회한다")
  @Test
  void getVerifications_withStatus_filtersStatus() {
    // given
    User admin = mock(User.class);
    given(admin.isAdmin()).willReturn(true);
    given(userRepository.findByIdAndDeletedAtIsNull(ADMIN_USER_ID)).willReturn(Optional.of(admin));

    PageRequest pageable = PageRequest.of(1, 10);
    given(exchangeVerificationRepository.findAllWithUserByStatus(
        ExchangeVerificationStatus.APPROVED,
        pageable
    )).willReturn(new SliceImpl<>(List.of(), pageable, false));

    // when
    ExchangeVerificationListResult result = service.getVerifications(
        ADMIN_USER_ID,
        ExchangeVerificationStatus.APPROVED,
        1,
        10
    );

    // then
    assertThat(result.content()).isEmpty();
    assertThat(result.page()).isEqualTo(1);
    assertThat(result.size()).isEqualTo(10);
    verify(exchangeVerificationRepository).findAllWithUserByStatus(
        ExchangeVerificationStatus.APPROVED,
        pageable
    );
  }

  @DisplayName("일반 사용자가 관리 목록을 조회하면 접근이 거부된다")
  @Test
  void getVerifications_nonAdmin_throwsForbidden() {
    // given
    User user = mock(User.class);
    given(user.isAdmin()).willReturn(false);
    given(userRepository.findByIdAndDeletedAtIsNull(ADMIN_USER_ID)).willReturn(Optional.of(user));

    // when & then
    assertThatThrownBy(() -> service.getVerifications(ADMIN_USER_ID, null, 0, 20))
        .isInstanceOf(BaseException.class)
        .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
            .isEqualTo(GlobalErrorCode.FORBIDDEN));
    verifyNoInteractions(exchangeVerificationRepository);
  }

  @DisplayName("관리자는 서류 인증 신청 상세 정보와 서류 열람 URL을 조회한다")
  @Test
  void getVerification_returnsDetail() {
    // given
    User admin = mock(User.class);
    given(admin.isAdmin()).willReturn(true);
    given(userRepository.findByIdAndDeletedAtIsNull(ADMIN_USER_ID)).willReturn(Optional.of(admin));

    LocalDateTime submittedAt = LocalDateTime.of(2026, 8, 30, 14, 20);
    ExchangeVerification verification = verification(submittedAt, ExchangeVerificationStatus.REJECTED);
    given(verification.getDocumentKey()).willReturn("exchange-verifications/2/document.pdf");
    given(verification.getOriginalFileName()).willReturn("교환학생 확인서.pdf");
    given(verification.getRejectionReason()).willReturn("서류가 확인되지 않습니다.");
    given(exchangeVerificationRepository.findByIdWithUser(10L))
        .willReturn(Optional.of(verification));
    given(s3PresignedUrlManager.createGetUrl("exchange-verifications/2/document.pdf"))
        .willReturn("https://example.com/presigned-document");

    // when
    ExchangeVerificationDetailResult result = service.getVerification(ADMIN_USER_ID, 10L);

    // then
    assertThat(result.verificationId()).isEqualTo(10L);
    assertThat(result.userId()).isEqualTo(APPLICANT_USER_ID);
    assertThat(result.nickname()).isEqualTo("지현");
    assertThat(result.submittedAt()).isEqualTo(submittedAt);
    assertThat(result.status()).isEqualTo(ExchangeVerificationStatus.REJECTED);
    assertThat(result.originalFileName()).isEqualTo("교환학생 확인서.pdf");
    assertThat(result.documentUrl()).isEqualTo("https://example.com/presigned-document");
    assertThat(result.rejectionReason()).isEqualTo("서류가 확인되지 않습니다.");
  }

  @DisplayName("존재하지 않는 서류 인증 신청을 상세 조회하면 예외가 발생한다")
  @Test
  void getVerification_notFound_throwsException() {
    // given
    User admin = mock(User.class);
    given(admin.isAdmin()).willReturn(true);
    given(userRepository.findByIdAndDeletedAtIsNull(ADMIN_USER_ID)).willReturn(Optional.of(admin));
    given(exchangeVerificationRepository.findByIdWithUser(999L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> service.getVerification(ADMIN_USER_ID, 999L))
        .isInstanceOf(BaseException.class)
        .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
            .isEqualTo(ExchangeVerificationErrorCode.VERIFICATION_NOT_FOUND));
    verifyNoInteractions(s3PresignedUrlManager);
  }

  @DisplayName("관리자는 대기 중인 서류 인증 신청을 승인한다")
  @Test
  void approveVerification_approvesVerificationAndUser() {
    // given
    User admin = admin();
    ExchangeVerification verification = verification(
        LocalDateTime.of(2026, 8, 30, 14, 20),
        ExchangeVerificationStatus.PENDING
    );
    User applicant = verification.getUser();
    given(verification.isPending()).willReturn(true);
    given(exchangeVerificationRepository.findByIdWithUserForUpdate(10L))
        .willReturn(Optional.of(verification));

    // when
    service.approveVerification(ADMIN_USER_ID, 10L);

    // then
    verify(verification).approve(admin);
    verify(applicant).verifyExchange();
  }

  @DisplayName("관리자는 기존 사용자 인증 상태를 변경하지 않고 대기 중인 신청을 반려한다")
  @Test
  void rejectVerification_rejectsVerificationAndUser() {
    // given
    User admin = admin();
    ExchangeVerification verification = verification(
        LocalDateTime.of(2026, 8, 30, 14, 20),
        ExchangeVerificationStatus.PENDING
    );
    given(verification.isPending()).willReturn(true);
    given(exchangeVerificationRepository.findByIdWithUserForUpdate(10L))
        .willReturn(Optional.of(verification));

    // when
    service.rejectVerification(ADMIN_USER_ID, 10L, "  서류가 확인되지 않습니다.  ");

    // then
    verify(verification).reject(admin, "서류가 확인되지 않습니다.");
    verify(verification, never()).getUser();
  }

  @DisplayName("이미 처리된 서류 인증 신청은 다시 처리할 수 없다")
  @Test
  void approveVerification_alreadyReviewed_throwsException() {
    // given
    admin();
    ExchangeVerification verification = verification(
        LocalDateTime.of(2026, 8, 30, 14, 20),
        ExchangeVerificationStatus.APPROVED
    );
    given(verification.isPending()).willReturn(false);
    given(exchangeVerificationRepository.findByIdWithUserForUpdate(10L))
        .willReturn(Optional.of(verification));

    // when & then
    assertThatThrownBy(() -> service.approveVerification(ADMIN_USER_ID, 10L))
        .isInstanceOf(BaseException.class)
        .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
            .isEqualTo(ExchangeVerificationErrorCode.VERIFICATION_ALREADY_REVIEWED));
  }

  private User admin() {
    User admin = mock(User.class);
    given(admin.isAdmin()).willReturn(true);
    given(userRepository.findByIdAndDeletedAtIsNull(ADMIN_USER_ID)).willReturn(Optional.of(admin));
    return admin;
  }

  private ExchangeVerification verification(
      LocalDateTime submittedAt,
      ExchangeVerificationStatus status
  ) {
    User applicant = mock(User.class);
    given(applicant.getId()).willReturn(APPLICANT_USER_ID);
    given(applicant.getNickname()).willReturn("지현");

    ExchangeVerification verification = mock(ExchangeVerification.class);
    given(verification.getId()).willReturn(10L);
    given(verification.getUser()).willReturn(applicant);
    given(verification.getUpdatedAt()).willReturn(submittedAt);
    given(verification.getStatus()).willReturn(status);
    return verification;
  }
}
