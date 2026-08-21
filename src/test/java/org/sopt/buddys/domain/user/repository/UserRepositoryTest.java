package org.sopt.buddys.domain.user.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.auth.entity.RefreshToken;
import org.sopt.buddys.domain.auth.repository.RefreshTokenRepository;
import org.sopt.buddys.domain.user.entity.AccountStatus;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.global.security.oauth.dto.KakaoUserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class UserRepositoryTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RefreshTokenRepository refreshTokenRepository;

  @AfterEach
  void tearDown() {
    userRepository.deleteAllInBatch();
  }

  @DisplayName("소셜 제공자와 제공자 ID로 사용자를 조회하면 해당 사용자가 반환된다")
  @Test
  void findByProviderAndProviderId_found() {
    // given
    User user = User.ofKakao("12345", createKakaoUserInfo());
    userRepository.save(user);

    // when
    Optional<User> result = userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "12345");

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getProviderId()).isEqualTo("12345");
  }

  @DisplayName("존재하지 않는 제공자 ID로 조회하면 빈 값이 반환된다")
  @Test
  void findByProviderAndProviderId_notFound() {
    // when
    Optional<User> result = userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "99999");

    // then
    assertThat(result).isEmpty();
  }

  @DisplayName("회원 상태 변경 후 리프레시 토큰을 삭제해도 soft delete 변경 사항이 저장된다")
  @Test
  void withdraw_thenDeleteRefreshToken_persistsSoftDelete() {
    // given
    User user = userRepository.save(User.ofKakao("12345", createKakaoUserInfo()));
    refreshTokenRepository.save(RefreshToken.of(user.getId(), "refresh-token", 60_000L));

    // when
    user.withdraw();
    refreshTokenRepository.deleteByUserId(user.getId());

    // then
    User withdrawnUser = userRepository.findById(user.getId()).orElseThrow();
    assertThat(withdrawnUser.getAccountStatus()).isEqualTo(AccountStatus.WITHDRAWN);
    assertThat(withdrawnUser.getDeletedAt()).isNotNull();
    assertThat(refreshTokenRepository.findById(user.getId())).isEmpty();
  }

  @DisplayName("탈퇴 회원을 익명화하면 동일한 소셜 계정으로 신규 회원을 생성할 수 있다")
  @Test
  void anonymizeWithdrawnUser_allowsSignupWithSameSocialAccount() {
    // given
    User withdrawnUser = userRepository.saveAndFlush(
        User.ofKakao("12345", createKakaoUserInfo())
    );

    // when
    withdrawnUser.withdraw();
    userRepository.saveAndFlush(withdrawnUser);
    User newUser = userRepository.saveAndFlush(
        User.ofKakao("12345", createKakaoUserInfo())
    );

    // then
    assertThat(newUser.getId()).isNotEqualTo(withdrawnUser.getId());
    assertThat(newUser.getProviderId()).isEqualTo("12345");
    assertThat(newUser.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
  }

  @DisplayName("탈퇴 표시용 닉네임을 다른 회원이 사용 중이어도 탈퇴할 수 있다")
  @Test
  void withdraw_displayNicknameAlreadyExists_usesUniqueInternalNickname() {
    // given
    User user = userRepository.saveAndFlush(User.ofKakao("12345", createKakaoUserInfo()));
    User nicknameOwner = User.builder()
        .provider(AuthProvider.GOOGLE)
        .providerId("google-user-id")
        .email("other@gmail.com")
        .nickname("탈퇴한 사용자_" + user.getId())
        .build();
    userRepository.saveAndFlush(nicknameOwner);

    // when
    user.withdraw();
    userRepository.saveAndFlush(user);

    // then
    assertThat(user.getNickname()).isNotEqualTo(nicknameOwner.getNickname());
    assertThat(user.getDisplayNickname()).isEqualTo("탈퇴한 사용자");
    assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.WITHDRAWN);
  }

  private KakaoUserInfo createKakaoUserInfo() {
    KakaoUserInfo.KakaoProfile profile = new KakaoUserInfo.KakaoProfile("닉네임", "http://img.url");
    KakaoUserInfo.KakaoAccount account = new KakaoUserInfo.KakaoAccount("test@kakao.com", profile);
    return new KakaoUserInfo(12345L, account);
  }
}
