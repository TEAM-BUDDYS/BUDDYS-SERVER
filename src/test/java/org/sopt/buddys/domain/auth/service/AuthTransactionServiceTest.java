package org.sopt.buddys.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.Optional;
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
import org.sopt.buddys.global.exception.BaseException;
import org.sopt.buddys.global.security.jwt.JwtProperties;
import org.sopt.buddys.global.security.jwt.JwtProvider;
import org.sopt.buddys.global.security.oauth.dto.KakaoUserInfo;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
public class AuthTransactionServiceTest {

  @InjectMocks
  private AuthTransactionService authTransactionService;

  @Mock
  private UserRepository userRepository;

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
    given(userRepository.save(any(User.class))).willReturn(savedUser);
    given(jwtProvider.generateToken(anyLong())).willReturn("jwt-token");
    given(jwtProvider.generateRefreshToken(anyLong())).willReturn("refresh-token");
    given(jwtProperties.refreshTokenExpiration()).willReturn(604800000L);

    // when
    AuthTokens result = authTransactionService.processKakaoLogin(providerId, kakaoUserInfo);

    // then
    assertThat(result.accessToken()).isEqualTo("jwt-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");
    then(userRepository).should(times(1)).save(any(User.class));
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

    // when
    AuthTokens result = authTransactionService.processKakaoLogin(providerId, kakaoUserInfo);

    // then
    assertThat(result.accessToken()).isEqualTo("jwt-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");
    then(userRepository).should(never()).save(any(User.class));
  }

  @DisplayName("닉네임 중복으로 회원가입에 실패하면 DUPLICATE_NICKNAME 예외가 발생한다")
  @Test
  void processKakaoLogin_duplicateNickname_throwsBaseException() {
    // given
    String providerId = "12345";
    KakaoUserInfo kakaoUserInfo = createKakaoUserInfo(providerId);

    given(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId)).willReturn(Optional.empty());
    given(userRepository.save(any(User.class))).willThrow(DataIntegrityViolationException.class);

    // when & then
    assertThatThrownBy(() -> authTransactionService.processKakaoLogin(providerId, kakaoUserInfo))
        .isInstanceOf(BaseException.class)
        .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
            .isEqualTo(AuthErrorCode.DUPLICATE_NICKNAME));
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

    // when
    authTransactionService.processKakaoLogin(providerId, kakaoUserInfo);

    // then - 기존 토큰 삭제 후 새 토큰 저장 순서 보장
    then(refreshTokenRepository).should(times(1)).deleteByUserId(1L);
    then(refreshTokenRepository).should(times(1)).save(any());
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