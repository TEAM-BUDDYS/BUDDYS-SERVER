package org.sopt.buddys.domain.user.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.user.controller.swagger.GetMyProfileSwagger;
import org.sopt.buddys.domain.user.dto.response.UserProfileResponse;
import org.sopt.buddys.domain.user.service.UserService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "사용자 API")
public class UserController {

  private final UserService userService;

  @GetMyProfileSwagger
  @GetMapping("/me")
  public BaseResponse<UserProfileResponse> getMyProfile(
      @Parameter(hidden = true)
      @LoginUser Long userId
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        userService.getProfile(userId)
    );
  }
}
