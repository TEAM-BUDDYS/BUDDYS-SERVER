package org.sopt.buddys.global.security.oauth.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.auth.code.AuthErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.sopt.buddys.global.security.oauth.dto.GoogleUserInfo;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class GoogleAuthClientTest {

  private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

  @Mock
  private RestTemplate restTemplate;

  @DisplayName("화이트리스트에 없는 redirect_uri로 요청하면 구글 API를 호출하지 않고 예외를 던진다")
  @Test
  void getAccessToken_rejectsRedirectUriNotInWhitelist() {
    // given
    GoogleOAuthProperties properties = new GoogleOAuthProperties(
        "client-id",
        "client-secret",
        List.of("http://localhost:3000/auth/google/callback"),
        "https://oauth2.googleapis.com/token",
        USER_INFO_URL
    );
    GoogleAuthClient googleAuthClient = new GoogleAuthClient(restTemplate, properties);

    // when & then
    assertThatThrownBy(() -> googleAuthClient.getAccessToken("code", "http://evil.example.com/callback"))
        .isInstanceOf(BaseException.class)
        .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
            .isEqualTo(AuthErrorCode.GOOGLE_REDIRECT_URI_NOT_ALLOWED));
    verifyNoInteractions(restTemplate);
  }

  @DisplayName("구글 이메일 인증 여부와 관계없이 사용자 정보를 반환한다")
  @Test
  void getUserInfo_returnsUnverifiedUser() {
    // given
    GoogleOAuthProperties properties = new GoogleOAuthProperties(
        "client-id",
        "client-secret",
        List.of("http://localhost:3000/auth/google/callback"),
        "https://oauth2.googleapis.com/token",
        USER_INFO_URL
    );
    GoogleAuthClient googleAuthClient = new GoogleAuthClient(restTemplate, properties);
    GoogleUserInfo unverifiedUser = new GoogleUserInfo(
        "google-user-id",
        "user@example.com",
        false,
        "사용자",
        null
    );
    given(restTemplate.exchange(
        eq(USER_INFO_URL),
        eq(HttpMethod.GET),
        any(HttpEntity.class),
        eq(GoogleUserInfo.class)
    )).willReturn(ResponseEntity.ok(unverifiedUser));

    // when
    GoogleUserInfo result = googleAuthClient.getUserInfo("access-token");

    // then
    assertThat(result).isEqualTo(unverifiedUser);
  }
}
