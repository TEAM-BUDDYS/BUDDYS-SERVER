package org.sopt.buddys.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.util.ReflectionTestUtils;
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

  @DisplayName("닉네임에 키워드가 부분 일치(대소문자 무시)하는 사용자를 검색한다")
  @Test
  void searchByNicknameContaining_partialMatchIgnoreCase() {
    // given
    User me = createUser("11111", "me@kakao.com", "나");
    User buddy = createUser("22222", "buddy@kakao.com", "Buddy1");
    User other = createUser("33333", "other@kakao.com", "여행자");
    userRepository.saveAll(List.of(me, buddy, other));

    // when
    Slice<User> result = userRepository.searchByNicknameContaining("buddy", me.getId(), PageRequest.of(0, 20));

    // then
    assertThat(result.getContent())
        .extracting(User::getNickname)
        .containsExactly("Buddy1");
  }

  @DisplayName("검색 결과에서 본인은 제외된다")
  @Test
  void searchByNicknameContaining_excludesSelf() {
    // given
    User me = createUser("11111", "me@kakao.com", "버디마스터");
    User other = createUser("22222", "other@kakao.com", "버디짱");
    userRepository.saveAll(List.of(me, other));

    // when
    Slice<User> result = userRepository.searchByNicknameContaining("버디", me.getId(), PageRequest.of(0, 20));

    // then
    assertThat(result.getContent())
        .extracting(User::getId)
        .containsExactly(other.getId());
  }

  @DisplayName("탈퇴한 사용자는 검색 결과에서 제외된다")
  @Test
  void searchByNicknameContaining_excludesDeletedUsers() {
    // given
    User me = createUser("11111", "me@kakao.com", "나");
    User deleted = createUser("22222", "deleted@kakao.com", "탈퇴버디");
    ReflectionTestUtils.setField(deleted, "deletedAt", java.time.LocalDateTime.now());
    userRepository.saveAll(List.of(me, deleted));

    // when
    Slice<User> result = userRepository.searchByNicknameContaining("버디", me.getId(), PageRequest.of(0, 20));

    // then
    assertThat(result.getContent()).isEmpty();
  }

  @DisplayName("검색 결과는 닉네임 오름차순으로 정렬된다")
  @Test
  void searchByNicknameContaining_ordersByNicknameAsc() {
    // given
    User me = createUser("11111", "me@kakao.com", "나");
    User buddyC = createUser("22222", "c@kakao.com", "버디c");
    User buddyA = createUser("33333", "a@kakao.com", "버디a");
    User buddyB = createUser("44444", "b@kakao.com", "버디b");
    userRepository.saveAll(List.of(me, buddyC, buddyA, buddyB));

    // when
    Slice<User> result = userRepository.searchByNicknameContaining("버디", me.getId(), PageRequest.of(0, 20));

    // then
    assertThat(result.getContent())
        .extracting(User::getNickname)
        .containsExactly("버디a", "버디b", "버디c");
  }

  private User createUser(String providerId, String email, String nickname) {
    KakaoUserInfo.KakaoProfile profile = new KakaoUserInfo.KakaoProfile(nickname, "http://img.url");
    KakaoUserInfo.KakaoAccount account = new KakaoUserInfo.KakaoAccount(email, profile);
    User user = User.ofKakao(providerId, new KakaoUserInfo(Long.parseLong(providerId), account));
    ReflectionTestUtils.setField(user, "nickname", nickname);
    return user;
  }

  private KakaoUserInfo createKakaoUserInfo() {
    KakaoUserInfo.KakaoProfile profile = new KakaoUserInfo.KakaoProfile("닉네임", "http://img.url");
    KakaoUserInfo.KakaoAccount account = new KakaoUserInfo.KakaoAccount("test@kakao.com", profile);
    return new KakaoUserInfo(12345L, account);
  }
}
