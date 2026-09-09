package org.sopt.buddys.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.user.entity.AccountStatus;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.global.security.oauth.dto.KakaoUserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
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

  @DisplayName("닉네임 검색에서 퍼센트 기호는 LIKE wildcard가 아닌 문자 그대로 조회된다")
  @Test
  void searchActiveUsersByNickname_percentIsTreatedAsLiteral() {
    User viewer = saveUser("viewer@test.com", "viewer", "viewer");
    User literalMatch = saveUser("percent@test.com", "percent", "Buddy%Match");
    saveUser("wildcard@test.com", "wildcard", "BuddyWildcardMatch");

    Slice<User> result = userRepository.searchActiveUsersByNickname(
        "%",
        viewer.getId(),
        AccountStatus.ACTIVE,
        PageRequest.of(0, 5)
    );

    assertThat(result.getContent()).extracting(User::getId).containsExactly(literalMatch.getId());
  }

  @DisplayName("닉네임 검색에서 밑줄 기호는 LIKE wildcard가 아닌 문자 그대로 조회된다")
  @Test
  void searchActiveUsersByNickname_underscoreIsTreatedAsLiteral() {
    User viewer = saveUser("viewer@test.com", "viewer", "viewer");
    User literalMatch = saveUser("underscore@test.com", "underscore", "Buddy_Match");
    saveUser("wildcard@test.com", "wildcard", "BuddyXMatch");

    Slice<User> result = userRepository.searchActiveUsersByNickname(
        "_",
        viewer.getId(),
        AccountStatus.ACTIVE,
        PageRequest.of(0, 5)
    );

    assertThat(result.getContent()).extracting(User::getId).containsExactly(literalMatch.getId());
  }

  private User saveUser(String email, String providerId, String nickname) {
    return userRepository.saveAndFlush(User.builder()
        .email(email)
        .provider(AuthProvider.KAKAO)
        .providerId(providerId)
        .nickname(nickname)
        .accountStatus(AccountStatus.ACTIVE)
        .build());
  }

  private KakaoUserInfo createKakaoUserInfo() {
    KakaoUserInfo.KakaoProfile profile = new KakaoUserInfo.KakaoProfile("닉네임", "http://img.url");
    KakaoUserInfo.KakaoAccount account = new KakaoUserInfo.KakaoAccount("test@kakao.com", profile);
    return new KakaoUserInfo(12345L, account);
  }
}
