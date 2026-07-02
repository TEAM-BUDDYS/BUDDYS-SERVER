package org.sopt.buddys.domain.user.controller;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.user.dto.response.UserProfileResponse;
import org.sopt.buddys.domain.user.service.UserService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  @GetMapping("/me")
  public BaseResponse<UserProfileResponse> getMyProfile(
      @RequestHeader(value = "X-USER-ID", defaultValue = "1") Long userId
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        userService.getMyProfile(userId)
    );
  }
}
