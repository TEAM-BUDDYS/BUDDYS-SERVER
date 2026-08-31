package org.sopt.buddys.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.auth.service.AuthService;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.jwt.JwtProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock
  private AuthService authService;

  @Mock
  private JwtProperties jwtProperties;

  private AuthController authController;

  @BeforeEach
  void setUp() {
    authController = new AuthController(authService, jwtProperties);
  }

  @DisplayName("로그아웃하면 리프레시 토큰을 폐기하고 쿠키를 만료시킨다")
  @Test
  void logout_deletesRefreshTokenAndExpiresCookie() {
    // given
    Long userId = 1L;
    HttpServletResponse servletResponse = new MockHttpServletResponse();

    // when
    ResponseEntity<BaseResponse<Void>> response = authController.logout(userId, servletResponse);

    // then
    then(authService).should(times(1)).logout(userId);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().success()).isTrue();

    String setCookie = servletResponse.getHeader(HttpHeaders.SET_COOKIE);
    assertThat(setCookie)
        .contains("refreshToken=")
        .contains("Path=/api/v1/auth/reissue")
        .contains("Max-Age=0")
        .contains("Secure")
        .contains("HttpOnly")
        .contains("SameSite=None");
  }
}
