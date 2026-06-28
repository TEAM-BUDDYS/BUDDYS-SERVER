package org.sopt.buddys.domain.auth.service;

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
import org.sopt.buddys.domain.auth.dto.response.LoginResponse;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
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
  private UserRepository userRepository;

  @Mock
  private JwtProvider jwtProvider;

  @DisplayName("신규 카카오 회원이 로그인하면 자동으로 회원가입되고 JWT가 발급된다")
  @Test
  void kakaoLogin_newUser() {
    // given
    String code = "kakao-auth-code";
    String kakaoAccessToken = "kakao-access-token";
    KakaoUserInfo kakaoUserInfo = createKakaoUserInfo("12345");
    User savedUser = createSavedKakaoUser(1L, "12345", kakaoUserInfo);

    given(kakaoAuthClient.getAccessToken(code)).willReturn(kakaoAccessToken);
    given(kakaoAuthClient.getUserInfo(kakaoAccessToken)).willReturn(kakaoUserInfo);
    given(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "12345")).willReturn(Optional.empty());
    given(userRepository.save(any(User.class))).willReturn(savedUser);
    given(jwtProvider.generateToken(anyLong())).willReturn("jwt-token");

    // when
    LoginResponse response = authService.kakaoLogin(code);

    // then
    assertThat(response.accessToken()).isEqualTo("jwt-token");
    then(userRepository).should(times(1)).save(any(User.class)); // 신규 저장 발생
  }

  @DisplayName("기존 카카오 회원이 로그인하면 회원가입 없이 JWT가 발급된다")
  @Test
  void kakaoLogin_existingUser() {
    // given
    String code = "kakao-auth-code";
    KakaoUserInfo kakaoUserInfo = createKakaoUserInfo("12345");
    User existingUser = createSavedKakaoUser(1L, "12345", kakaoUserInfo);

    given(kakaoAuthClient.getAccessToken(code)).willReturn("kakao-access-token");

    given(kakaoAuthClient.getUserInfo("kakao-access-token")).willReturn(kakaoUserInfo);
    given(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "12345")).willReturn(Optional.of(existingUser));
    given(jwtProvider.generateToken(any())).willReturn("jwt-token");

    // when
    LoginResponse response = authService.kakaoLogin(code);

    // then
    assertThat(response.accessToken()).isEqualTo("jwt-token");
    then(userRepository).should(never()).save(any(User.class)); // 저장 없음
  }

  private KakaoUserInfo createKakaoUserInfo(String id) {
    KakaoUserInfo.KakaoProfile profile = new KakaoUserInfo.KakaoProfile("닉네임","http://img.url");
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
