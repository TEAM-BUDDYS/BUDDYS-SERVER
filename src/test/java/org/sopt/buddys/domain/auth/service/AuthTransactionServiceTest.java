package org.sopt.buddys.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.sql.SQLException;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.auth.code.AuthErrorCode;
import org.sopt.buddys.domain.auth.dto.response.AuthTokens;
import org.sopt.buddys.domain.auth.repository.RefreshTokenRepository;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.service.UserService;
import org.sopt.buddys.global.exception.BaseException;
import org.sopt.buddys.global.security.jwt.JwtProperties;
import org.sopt.buddys.global.security.jwt.JwtProvider;
import org.sopt.buddys.global.security.oauth.dto.GoogleUserInfo;
import org.sopt.buddys.global.security.oauth.dto.KakaoUserInfo;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
public class AuthTransactionServiceTest {

  @InjectMocks
  private AuthTransactionService authTransactionService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private SocialUserTransactionService socialUserTransactionService;

  @Mock
  private UserService userService;

  @Mock
  private JwtProvider jwtProvider;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private JwtProperties jwtProperties;

  @DisplayName("신규 카카오 회원이 로그인하면 자동으로 회원가입되고 토큰이 발급된다")
  @Test
  void processKakaoLogin_newUser_savesUserAndIssuesTokens() {
    // given
    String providerId = "12345";
    KakaoUserInfo kakaoUserInfo = createKakaoUserInfo(providerId);
    User savedUser = createSavedKakaoUser(1L, providerId, kakaoUserInfo);

    given(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId)).willReturn(Optional.empty());
    given(socialUserTransactionService.create(any(User.class))).willReturn(savedUser);
    given(jwtProvider.generateToken(anyLong())).willReturn("jwt-token");
    given(jwtProvider.generateRefreshToken(anyLong())).willReturn("refresh-token");
    given(jwtProperties.refreshTokenExpiration()).willReturn(604800000L);
    given(userService.isOnboardingCompleted(any(User.class))).willReturn(false);

    // when
    AuthTokens result = authTransactionService.processKakaoLogin(providerId, kakaoUserInfo);

    // then
    assertThat(result.accessToken()).isEqualTo("jwt-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");
    then(socialUserTransactionService).should(times(1)).create(any(User.class));
  }

  @DisplayName("기존 카카오 회원이 로그인하면 회원가입 없이 토큰이 발급된다")
  @Test
  void processKakaoLogin_existingUser_skipsSaveAndIssuesTokens() {
    // given
    String providerId = "12345";
    KakaoUserInfo kakaoUserInfo = createKakaoUserInfo(providerId);
    User existingUser = createSavedKakaoUser(1L, providerId, kakaoUserInfo);

    given(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId)).willReturn(Optional.of(existingUser));
    given(jwtProvider.generateToken(anyLong())).willReturn("jwt-token");
    given(jwtProvider.generateRefreshToken(anyLong())).willReturn("refresh-token");
    given(jwtProperties.refreshTokenExpiration()).willReturn(604800000L);
    given(userService.isOnboardingCompleted(any(User.class))).willReturn(false);

    // when
    AuthTokens result = authTransactionService.processKakaoLogin(providerId, kakaoUserInfo);

    // then
    assertThat(result.accessToken()).isEqualTo("jwt-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");
    then(socialUserTransactionService).should(never()).create(any(User.class));
  }

  @DisplayName("닉네임 중복으로 회원가입에 실패하면 DUPLICATE_NICKNAME 예외가 발생한다")
  @Test
  void processKakaoLogin_duplicateNickname_throwsBaseException() {
    // given
    String providerId = "12345";
    KakaoUserInfo kakaoUserInfo = createKakaoUserInfo(providerId);

    given(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId)).willReturn(Optional.empty());
    ConstraintViolationException nicknameConstraintViolation = new ConstraintViolationException(
        "duplicate nickname", new SQLException("duplicate"), "uk_user_nickname"
    );
    given(socialUserTransactionService.create(any(User.class))).willThrow(
        new DataIntegrityViolationException("duplicate", nicknameConstraintViolation)
    );

    // when & then
    assertThatThrownBy(() -> authTransactionService.processKakaoLogin(providerId, kakaoUserInfo))
        .isInstanceOf(BaseException.class)
        .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
            .isEqualTo(AuthErrorCode.DUPLICATE_NICKNAME));
  }

  @DisplayName("동시 최초 로그인으로 provider 유니크 키가 충돌하면 생성된 사용자를 다시 조회해 로그인한다")
  @Test
  void processGoogleLogin_duplicateProvider_recoversAsExistingUser() {
    // given
    String providerId = "google-user-id";
    GoogleUserInfo googleUserInfo = new GoogleUserInfo(
        providerId,
        "test@gmail.com",
        true,
        "사용자",
        "http://img.url"
    );
    User concurrentlyCreatedUser = User.builder()
        .id(1L)
        .provider(AuthProvider.GOOGLE)
        .providerId(providerId)
        .email(googleUserInfo.email())
        .nickname("닉네임")
        .profileImageUrl(googleUserInfo.picture())
        .build();
    ConstraintViolationException providerConstraintViolation = new ConstraintViolationException(
        "duplicate provider", new SQLException("duplicate"), "user.uk_user_provider"
    );

    given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, providerId))
        .willReturn(Optional.empty());
    given(socialUserTransactionService.create(any(User.class))).willThrow(
        new DataIntegrityViolationException("duplicate", providerConstraintViolation)
    );
    given(socialUserTransactionService.findByProviderAndProviderId(
        AuthProvider.GOOGLE, providerId
    )).willReturn(Optional.of(concurrentlyCreatedUser));
    given(jwtProvider.generateToken(1L)).willReturn("jwt-token");
    given(jwtProvider.generateRefreshToken(1L)).willReturn("refresh-token");
    given(jwtProperties.refreshTokenExpiration()).willReturn(604800000L);
    given(userService.isOnboardingCompleted(concurrentlyCreatedUser)).willReturn(false);

    // when
    AuthTokens result = authTransactionService.processGoogleLogin(providerId, googleUserInfo);

    // then
    assertThat(result.userId()).isEqualTo(1L);
    assertThat(result.accessToken()).isEqualTo("jwt-token");
    then(socialUserTransactionService).should().findByProviderAndProviderId(
        AuthProvider.GOOGLE, providerId
    );
  }

  @DisplayName("닉네임과 provider 이외의 제약 위반은 원본 예외를 그대로 전파한다")
  @Test
  void processKakaoLogin_otherConstraint_rethrowsOriginalException() {
    // given
    String providerId = "12345";
    KakaoUserInfo kakaoUserInfo = createKakaoUserInfo(providerId);
    ConstraintViolationException otherConstraintViolation = new ConstraintViolationException(
        "other constraint", new SQLException("constraint violation"), "fk_user_country"
    );
    DataIntegrityViolationException originalException = new DataIntegrityViolationException(
        "constraint violation", otherConstraintViolation
    );

    given(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId))
        .willReturn(Optional.empty());
    given(socialUserTransactionService.create(any(User.class))).willThrow(originalException);

    // when & then
    assertThatThrownBy(() -> authTransactionService.processKakaoLogin(providerId, kakaoUserInfo))
        .isSameAs(originalException);
    then(socialUserTransactionService).should(never()).findByProviderAndProviderId(
        AuthProvider.KAKAO, providerId
    );
  }

  @DisplayName("로그인 시 기존 리프레시 토큰을 삭제하고 새로운 토큰을 저장한다")
  @Test
  void processKakaoLogin_replacesRefreshToken() {
    // given
    String providerId = "12345";
    KakaoUserInfo kakaoUserInfo = createKakaoUserInfo(providerId);
    User existingUser = createSavedKakaoUser(1L, providerId, kakaoUserInfo);

    given(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId)).willReturn(Optional.of(existingUser));
    given(jwtProvider.generateToken(anyLong())).willReturn("jwt-token");
    given(jwtProvider.generateRefreshToken(anyLong())).willReturn("refresh-token");
    given(jwtProperties.refreshTokenExpiration()).willReturn(604800000L);
    given(userService.isOnboardingCompleted(any(User.class))).willReturn(false);

    // when
    authTransactionService.processKakaoLogin(providerId, kakaoUserInfo);

    // then - 기존 토큰 삭제 후 새 토큰 저장 순서 보장
    then(refreshTokenRepository).should(times(1)).deleteByUserId(1L);
    then(refreshTokenRepository).should(times(1)).save(any());
  }

  @DisplayName("기존 구글 회원은 이메일 인증 값이 false여도 토큰이 발급된다")
  @Test
  void processGoogleLogin_existingUser_issuesTokens() {
    // given
    String providerId = "google-user-id";
    GoogleUserInfo googleUserInfo = new GoogleUserInfo(
        providerId,
        "test@gmail.com",
        false,
        "사용자",
        "http://img.url"
    );
    User existingUser = User.builder()
        .id(1L)
        .provider(AuthProvider.GOOGLE)
        .providerId(providerId)
        .email(googleUserInfo.email())
        .nickname("닉네임")
        .profileImageUrl(googleUserInfo.picture())
        .build();

    given(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, providerId))
        .willReturn(Optional.of(existingUser));
    given(jwtProvider.generateToken(1L)).willReturn("jwt-token");
    given(jwtProvider.generateRefreshToken(1L)).willReturn("refresh-token");
    given(jwtProperties.refreshTokenExpiration()).willReturn(604800000L);
    given(userService.isOnboardingCompleted(existingUser)).willReturn(false);

    // when
    AuthTokens result = authTransactionService.processGoogleLogin(providerId, googleUserInfo);

    // then
    assertThat(result.accessToken()).isEqualTo("jwt-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");
    then(socialUserTransactionService).should(never()).create(any(User.class));
  }

  private KakaoUserInfo createKakaoUserInfo(String id) {
    KakaoUserInfo.KakaoProfile profile = new KakaoUserInfo.KakaoProfile("닉네임", "http://img.url");
    KakaoUserInfo.KakaoAccount account = new KakaoUserInfo.KakaoAccount("test@kakao.com", profile);
    return new KakaoUserInfo(Long.parseLong(id), account);
  }

  private User createSavedKakaoUser(Long id, String providerId, KakaoUserInfo kakaoUserInfo) {
    return User.builder()
        .id(id)
        .provider(AuthProvider.KAKAO)
        .providerId(providerId)
        .email(kakaoUserInfo.kakaoAccount().email())
        .nickname(kakaoUserInfo.kakaoAccount().profile().nickname())
        .profileImageUrl(kakaoUserInfo.kakaoAccount().profile().profileImageUrl())
        .build();
  }
}
