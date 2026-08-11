package org.sopt.buddys.global.security.oauth.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.auth.code.AuthErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class KakaoAuthClientTest {

  @Mock
  private RestTemplate restTemplate;

  @DisplayName("화이트리스트에 없는 redirect_uri로 요청하면 카카오 API를 호출하지 않고 예외를 던진다")
  @Test
  void getAccessToken_rejectsRedirectUriNotInWhitelist() {
    // given
    KakaoOAuthProperties properties = new KakaoOAuthProperties(
        "client-id",
        "client-secret",
        List.of("http://localhost:3000/auth/kakao/callback"),
        "https://kauth.kakao.com/oauth/token",
        "https://kapi.kakao.com/v2/user/me"
    );
    KakaoAuthClient kakaoAuthClient = new KakaoAuthClient(restTemplate, properties);

    // when & then
    assertThatThrownBy(() -> kakaoAuthClient.getAccessToken("code", "http://evil.example.com/callback"))
        .isInstanceOf(BaseException.class)
        .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
            .isEqualTo(AuthErrorCode.KAKAO_REDIRECT_URI_NOT_ALLOWED));
    verifyNoInteractions(restTemplate);
  }
}