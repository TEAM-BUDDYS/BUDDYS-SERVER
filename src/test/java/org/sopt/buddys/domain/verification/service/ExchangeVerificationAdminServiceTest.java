package org.sopt.buddys.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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
import org.sopt.buddys.domain.verification.entity.ExchangeVerification;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;
import org.sopt.buddys.domain.verification.repository.ExchangeVerificationRepository;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationListResult;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

class ExchangeVerificationAdminServiceTest {

  private static final long ADMIN_USER_ID = 1L;
  private static final long APPLICANT_USER_ID = 2L;

  private ExchangeVerificationRepository exchangeVerificationRepository;
  private UserRepository userRepository;
  private ExchangeVerificationAdminService service;

  @BeforeEach
  void setUp() {
    exchangeVerificationRepository = mock(ExchangeVerificationRepository.class);
    userRepository = mock(UserRepository.class);
    service = new ExchangeVerificationAdminService(exchangeVerificationRepository, userRepository);
  }

  @DisplayName("관리자는 전체 서류 인증 신청을 최신순 페이지로 조회한다")
  @Test
  void getVerifications_withoutStatus_returnsAll() {
    // given
    User admin = mock(User.class);
    given(admin.isAdmin()).willReturn(true);
    given(userRepository.findByIdAndDeletedAtIsNull(ADMIN_USER_ID)).willReturn(Optional.of(admin));

    LocalDateTime submittedAt = LocalDateTime.of(2026, 8, 30, 14, 20);
    ExchangeVerification verification = verification(submittedAt, ExchangeVerificationStatus.PENDING);
    PageRequest pageable = PageRequest.of(0, 20);
    given(exchangeVerificationRepository.findLatestByUser(pageable))
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
    given(exchangeVerificationRepository.findLatestByUserAndStatus(
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
    verify(exchangeVerificationRepository).findLatestByUserAndStatus(
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
