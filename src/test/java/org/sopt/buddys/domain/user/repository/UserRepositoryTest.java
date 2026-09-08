package org.sopt.buddys.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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

  @DisplayName("이스케이프된 언더스코어(_)는 단일 문자 와일드카드가 아닌 일반 문자로 매칭된다")
  @Test
  void searchByNicknameContaining_escapedUnderscore_matchesLiterally() {
    // given
    User me = createUser("11111", "me@kakao.com", "나");
    User exactMatch = createUser("22222", "exact@kakao.com", "버디_1");
    User wildcardVictim = createUser("33333", "wildcard@kakao.com", "버디21");
    userRepository.saveAll(List.of(me, exactMatch, wildcardVictim));

    // when
    Slice<User> result = userRepository.searchByNicknameContaining("버디\\_1", me.getId(), PageRequest.of(0, 20));

    // then
    assertThat(result.getContent())
        .extracting(User::getNickname)
        .containsExactly("버디_1");
  }

  @DisplayName("이스케이프된 퍼센트(%)는 임의 문자열 와일드카드가 아닌 일반 문자로 매칭된다")
  @Test
  void searchByNicknameContaining_escapedPercent_matchesLiterally() {
    // given
    User me = createUser("11111", "me@kakao.com", "나");
    User exactMatch = createUser("22222", "exact@kakao.com", "버디%1");
    User wildcardVictim = createUser("33333", "wildcard@kakao.com", "버디아무거나1");
    userRepository.saveAll(List.of(me, exactMatch, wildcardVictim));

    // when
    Slice<User> result = userRepository.searchByNicknameContaining("버디\\%1", me.getId(), PageRequest.of(0, 20));

    // then
    assertThat(result.getContent())
        .extracting(User::getNickname)
        .containsExactly("버디%1");
  }

  @DisplayName("이스케이프된 백슬래시(\\)는 이스케이프 문자가 아닌 일반 문자로 매칭된다")
  @Test
  void searchByNicknameContaining_escapedBackslash_matchesLiterally() {
    // given
    User me = createUser("11111", "me@kakao.com", "나");
    User exactMatch = createUser("22222", "exact@kakao.com", "버디\\1");
    User wildcardVictim = createUser("33333", "wildcard@kakao.com", "버디1");
    userRepository.saveAll(List.of(me, exactMatch, wildcardVictim));

    // when
    Slice<User> result = userRepository.searchByNicknameContaining("버디\\\\1", me.getId(), PageRequest.of(0, 20));

    // then
    assertThat(result.getContent())
        .extracting(User::getNickname)
        .containsExactly("버디\\1");
  }

  private User createUser(String providerId, String email, String nickname) {
    KakaoUserInfo.KakaoProfile profile = new KakaoUserInfo.KakaoProfile(nickname, "http://img.url");
    KakaoUserInfo.KakaoAccount account = new KakaoUserInfo.KakaoAccount(email, profile);
    User user = User.ofKakao(providerId, new KakaoUserInfo(Long.parseLong(providerId), account));
    ReflectionTestUtils.setField(user, "nickname", nickname);
    return user;
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
