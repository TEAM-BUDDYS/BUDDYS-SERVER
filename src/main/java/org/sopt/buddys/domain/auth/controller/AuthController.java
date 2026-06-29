package org.sopt.buddys.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.sopt.buddys.domain.auth.code.AuthErrorCode;
import org.sopt.buddys.domain.auth.dto.response.AuthTokens;
import org.sopt.buddys.domain.auth.dto.response.LoginResponse;
import org.sopt.buddys.domain.auth.service.AuthService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.exception.BaseException;
import org.sopt.buddys.global.response.BaseResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;
  private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @Operation(summary = "카카오 로그인", description = "카카오 인가 코드를 이용해 로그인하고 JWT를 발급합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "로그인 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 인가 코드"),
  })
  @PostMapping("/kakao")
  public ResponseEntity<BaseResponse<LoginResponse>> kakaoLogin(
      @Parameter(description = "카카오 OAuth 인가 코드", example = "kaO7zQSSJXT...")
      @RequestParam String code,
      HttpServletResponse response
  ) {
    AuthTokens tokens = authService.kakaoLogin(code);
    addRefreshTokenCookie(response, tokens.refreshToken());
    return ResponseEntity.ok(BaseResponse.success(GlobalSuccessCode.OK, new LoginResponse(tokens.accessToken())));
  }

  @Operation(summary = "JWT 재발급", description = "리프레시 토큰을 이용해 새로운 JWT를 발급합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "JWT 재발급 성공"),
      @ApiResponse(responseCode = "401", description = "리프레시 토큰이 유효하지 않음"),
  })
  @PostMapping("/reissue")
  public ResponseEntity<BaseResponse<LoginResponse>> reissue(
      @Parameter(in = ParameterIn.COOKIE, name = "refreshToken", description = "리프레시 토큰")
      @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
      HttpServletResponse response
  ) {
    if (refreshToken == null) {
      throw new BaseException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }
    AuthTokens tokens = authService.reissue(refreshToken);
    addRefreshTokenCookie(response, tokens.refreshToken());
    return ResponseEntity.ok(BaseResponse.success(GlobalSuccessCode.OK, new LoginResponse(tokens.accessToken())));
  }

  private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
    ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
        .httpOnly(true)
        .secure(true)
        .path("/api/v1/auth/reissue")
        .maxAge(Duration.ofDays(7))
        .sameSite("Strict")
        .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

}
