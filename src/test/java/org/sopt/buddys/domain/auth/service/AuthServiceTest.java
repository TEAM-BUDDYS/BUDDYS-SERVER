package org.sopt.buddys.domain.auth.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.auth.dto.response.AuthTokens;
import org.sopt.buddys.domain.auth.repository.RefreshTokenRepository;
import org.sopt.buddys.global.security.jwt.JwtProperties;
import org.sopt.buddys.global.security.jwt.JwtProvider;
import org.sopt.buddys.global.security.oauth.dto.KakaoUserInfo;
import org.sopt.buddys.global.security.oauth.kakao.KakaoAuthClient;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

  @InjectMocks
  private AuthService authService;

  @Mock
  private KakaoAuthClient kakaoAuthClient;

  @Mock
  private AuthTransactionService authTransactionService;

  @Mock
  private JwtProvider jwtProvider;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private JwtProperties jwtProperties;

  @DisplayName("카카오 인가 코드로 로그인하면 카카오 API를 호출하고 트랜잭션 서비스에 위임한다")
  @Test
  void kakaoLogin_callsKakaoApiAndDelegates() {
    // given
    String code = "kakao-auth-code";
    String redirectUri = "http://localhost:3000/oauth/callback";
    String kakaoAccessToken = "kakao-access-token";
    KakaoUserInfo kakaoUserInfo = createKakaoUserInfo("12345");
    AuthTokens expectedTokens = new AuthTokens(1L, "jwt-token", "refresh-token", false);

    given(kakaoAuthClient.getAccessToken(code, redirectUri)).willReturn(kakaoAccessToken);
    given(kakaoAuthClient.getUserInfo(kakaoAccessToken)).willReturn(kakaoUserInfo);
    given(authTransactionService.processKakaoLogin("12345", kakaoUserInfo)).willReturn(expectedTokens);

    // when
    AuthTokens response = authService.kakaoLogin(code, redirectUri);

    // then
    assertThat(response.userId()).isEqualTo(1L);
    assertThat(response.accessToken()).isEqualTo("jwt-token");
    assertThat(response.refreshToken()).isEqualTo("refresh-token");
    then(kakaoAuthClient).should(times(1)).getAccessToken(code, redirectUri);
    then(kakaoAuthClient).should(times(1)).getUserInfo(kakaoAccessToken);
    then(authTransactionService).should(times(1)).processKakaoLogin("12345", kakaoUserInfo);
  }

  @DisplayName("로그아웃하면 사용자의 리프레시 토큰을 삭제한다")
  @Test
  void logout_deletesRefreshTokenByUserId() {
    // given
    Long userId = 1L;

    // when
    authService.logout(userId);

    // then
    then(refreshTokenRepository).should(times(1)).deleteByUserId(userId);
  }

  private KakaoUserInfo createKakaoUserInfo(String id) {
    KakaoUserInfo.KakaoProfile profile = new KakaoUserInfo.KakaoProfile("닉네임", "http://img.url");
    KakaoUserInfo.KakaoAccount account = new KakaoUserInfo.KakaoAccount("test@kakao.com", profile);
    return new KakaoUserInfo(Long.parseLong(id), account);
  }
}
