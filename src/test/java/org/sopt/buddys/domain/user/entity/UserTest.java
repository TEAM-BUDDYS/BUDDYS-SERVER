package org.sopt.buddys.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.auth.code.AuthErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.sopt.buddys.global.security.oauth.dto.GoogleUserInfo;
import org.sopt.buddys.global.security.oauth.dto.KakaoUserInfo;

public class UserTest {

  @DisplayName("회원을 탈퇴시키면 개인정보를 익명화하고 탈퇴 상태로 변경한다")
  @Test
  void withdraw_anonymizesPersonalDataAndChangesAccountStatus() {
    // given
    User user = User.builder()
        .id(1L)
        .provider(AuthProvider.KAKAO)
        .providerId("12345")
        .email("test@kakao.com")
        .nickname("버디")
        .profileImageUrl("https://example.com/profile.png")
        .introduction("자기소개")
        .birthDate(LocalDate.of(2000, 1, 1))
        .gender(Gender.FEMALE)
        .universityVerified(true)
        .exchangeVerified(true)
        .build();
    LocalDateTime beforeWithdrawal = LocalDateTime.now();

    // when
    user.withdraw();

    // then
    assertThat(user.getProviderId()).startsWith("withdrawn:").isNotEqualTo("12345");
    assertThat(user.getEmail()).endsWith("@deleted.invalid").isNotEqualTo("test@kakao.com");
    assertThat(user.getNickname()).startsWith("탈퇴한 사용자_").hasSize(40);
    assertThat(user.getDisplayNickname()).isEqualTo("탈퇴한 사용자");
    assertThat(user.getNickname()).hasSizeLessThanOrEqualTo(50);
    assertThat(user.getProfileImageUrl()).isNull();
    assertThat(user.getIntroduction()).isNull();
    assertThat(user.getBirthDate()).isNull();
    assertThat(user.getGender()).isNull();
    assertThat(user.isNotificationEnabled()).isFalse();
    assertThat(user.isUniversityVerified()).isFalse();
    assertThat(user.isExchangeVerified()).isFalse();
    assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.WITHDRAWN);
    assertThat(user.getDeletedAt()).isAfterOrEqualTo(beforeWithdrawal);
  }

  @DisplayName("카카오 계정 정보로 회원을 생성하면 카카오 제공자 정보와 기본 상태가 설정된다")
  @Test
  void createUserFromKakaoInfo() {
    // given
    KakaoUserInfo.KakaoProfile profile = new KakaoUserInfo.KakaoProfile("닉네임", "http://img.url");
    KakaoUserInfo.KakaoAccount account = new KakaoUserInfo.KakaoAccount("test@kakao.com", profile);
    KakaoUserInfo kakaoUserInfo = new KakaoUserInfo(12345L, account);

    // when
    User user = User.ofKakao("12345", kakaoUserInfo);

    // then
    assertThat(user.getProvider()).isEqualTo(AuthProvider.KAKAO);
    assertThat(user.getProviderId()).isEqualTo("12345");
    assertThat(user.getEmail()).isEqualTo("test@kakao.com");
    assertThat(user.getNickname()).isNotBlank();
    assertThat(user.getProfileImageUrl()).isEqualTo("http://img.url");
    assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(user.isNotificationEnabled()).isTrue();
    assertThat(user.isUniversityVerified()).isFalse();
    assertThat(user.isExchangeVerified()).isFalse();
  }

  @DisplayName("이메일이 인증되지 않은 구글 계정으로 신규 회원을 생성할 수 없다")
  @Test
  void createUserFromGoogleInfo_rejectsUnverifiedEmail() {
    // given
    GoogleUserInfo googleUserInfo = new GoogleUserInfo(
        "google-user-id",
        "test@gmail.com",
        false,
        "사용자",
        "http://img.url"
    );

    // when & then
    assertThatThrownBy(() -> User.ofGoogle("google-user-id", googleUserInfo))
        .isInstanceOf(BaseException.class)
        .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
            .isEqualTo(AuthErrorCode.GOOGLE_EMAIL_NOT_VERIFIED));
  }

}
