package org.sopt.buddys.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.user.event.UserWithdrawnEvent;
import org.sopt.buddys.domain.verification.repository.UniversityVerificationRepository;

class UserWithdrawalVerificationListenerTest {

  private final UniversityVerificationRepository universityVerificationRepository =
      org.mockito.Mockito.mock(UniversityVerificationRepository.class);
  private final UserWithdrawalVerificationListener listener =
      new UserWithdrawalVerificationListener(universityVerificationRepository);

  @DisplayName("회원 탈퇴 커밋 후 Redis 학교 인증 정보를 삭제한다")
  @Test
  void deleteUniversityVerification_deletesByUserId() {
    // when
    listener.deleteUniversityVerification(new UserWithdrawnEvent(1L));

    // then
    then(universityVerificationRepository).should().deleteByUserId(1L);
  }

  @DisplayName("Redis 삭제가 실패해도 이미 커밋된 회원 탈퇴 처리는 실패로 바꾸지 않는다")
  @Test
  void deleteUniversityVerification_redisFailure_doesNotPropagate() {
    // given
    willThrow(new IllegalStateException("redis unavailable"))
        .given(universityVerificationRepository).deleteByUserId(1L);

    // when & then
    assertThatCode(() -> listener.deleteUniversityVerification(new UserWithdrawnEvent(1L)))
        .doesNotThrowAnyException();
  }
}
