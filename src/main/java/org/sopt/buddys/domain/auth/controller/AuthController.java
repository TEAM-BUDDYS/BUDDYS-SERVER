package org.sopt.buddys.domain.auth.controller;

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

  @PostMapping("/kakao")
  public ResponseEntity<BaseResponse<LoginResponse>> kakaoLogin(@RequestParam String code) {
    return ResponseEntity.ok(BaseResponse.success(GlobalSuccessCode.OK, authService.kakaoLogin(code)));
  }

}
