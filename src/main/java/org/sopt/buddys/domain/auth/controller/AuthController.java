package org.sopt.buddys.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.sopt.buddys.domain.auth.dto.response.LoginResponse;
import org.sopt.buddys.domain.auth.service.AuthService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

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
      @RequestParam String code
  ) {
    return ResponseEntity.ok(BaseResponse.success(GlobalSuccessCode.OK, authService.kakaoLogin(code)));
  }

}
