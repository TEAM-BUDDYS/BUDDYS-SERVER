package org.sopt.buddys.domain.user.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.user.controller.swagger.GetMyPostsSwagger;
import org.sopt.buddys.domain.user.controller.swagger.GetMyProfileSwagger;
import org.sopt.buddys.domain.user.controller.swagger.GetUserProfileSwagger;
import org.sopt.buddys.domain.user.dto.response.UserPostsResponse;
import org.sopt.buddys.domain.user.dto.response.UserProfileResponse;
import org.sopt.buddys.domain.user.dto.response.UserPublicProfileResponse;
import org.sopt.buddys.domain.user.service.UserService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        UserProfileResponse.from(userService.getProfile(userId))
    );
  }

  @GetMyPostsSwagger
  @GetMapping("/me/posts")
  public BaseResponse<UserPostsResponse> getMyPosts(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "페이지 번호. 0부터 시작합니다.", example = "0")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "페이지 크기", example = "10")
      @RequestParam(defaultValue = "10") int size
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        UserPostsResponse.from(userService.getPosts(userId, page, size))
    );
  }

  @GetUserProfileSwagger
  @GetMapping("/{userId}")
  public BaseResponse<UserPublicProfileResponse> getUserProfile(
      @Parameter(description = "조회할 사용자 ID", example = "1")
      @PathVariable Long userId
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        UserPublicProfileResponse.from(userService.getProfile(userId))
    );
  }
}
