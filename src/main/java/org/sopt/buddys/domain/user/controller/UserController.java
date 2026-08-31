package org.sopt.buddys.domain.user.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.user.controller.swagger.CheckNicknameAvailabilitySwagger;
import org.sopt.buddys.domain.user.controller.swagger.CompleteOnboardingSwagger;
import org.sopt.buddys.domain.user.controller.swagger.GetMyCoursesSwagger;
import org.sopt.buddys.domain.user.controller.swagger.GetMyPostsSwagger;
import org.sopt.buddys.domain.user.controller.swagger.GetMyProfileSwagger;
import org.sopt.buddys.domain.user.controller.swagger.GetProfileEditSwagger;
import org.sopt.buddys.domain.user.controller.swagger.GetNotificationSettingSwagger;
import org.sopt.buddys.domain.user.controller.swagger.GetUserCoursesSwagger;
import org.sopt.buddys.domain.user.controller.swagger.GetUserPostsSwagger;
import org.sopt.buddys.domain.user.controller.swagger.GetUserProfileSwagger;
import org.sopt.buddys.domain.user.controller.swagger.UpdateProfileSwagger;
import org.sopt.buddys.domain.user.controller.swagger.UpdateNotificationSettingSwagger;
import org.sopt.buddys.domain.user.dto.request.OnboardingRequest;
import org.sopt.buddys.domain.user.dto.request.UpdateProfileRequest;
import org.sopt.buddys.domain.user.dto.response.NicknameAvailabilityResponse;
import org.sopt.buddys.domain.user.dto.request.UpdateNotificationSettingRequest;
import org.sopt.buddys.domain.user.dto.response.NotificationSettingResponse;
import org.sopt.buddys.domain.user.dto.response.OnboardingResponse;
import org.sopt.buddys.domain.user.dto.response.ProfileEditResponse;
import org.sopt.buddys.domain.user.dto.response.UserCoursesResponse;
import org.sopt.buddys.domain.user.dto.response.UserPostsResponse;
import org.sopt.buddys.domain.user.dto.response.UserProfileResponse;
import org.sopt.buddys.domain.user.dto.response.UserPublicProfileResponse;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.service.UserOnboardingService;
import org.sopt.buddys.domain.user.service.UserProfileEditService;
import org.sopt.buddys.domain.user.service.UserService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "사용자 API")
public class UserController {

  private final UserService userService;
  private final UserOnboardingService userOnboardingService;
  private final UserProfileEditService userProfileEditService;

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

  @GetProfileEditSwagger
  @GetMapping("/me/edit")
  public BaseResponse<ProfileEditResponse> getMyProfileForEdit(
          @Parameter(hidden = true)
          @LoginUser Long userId
  ) {
    return BaseResponse.success(GlobalSuccessCode.OK, userProfileEditService.getProfile(userId));
  }

  @UpdateProfileSwagger
  @PutMapping("/me")
  public BaseResponse<ProfileEditResponse> updateMyProfile(
          @Parameter(hidden = true)
          @LoginUser Long userId,
          @RequestBody @Valid UpdateProfileRequest request
  ) {
    return BaseResponse.success(
            GlobalSuccessCode.OK,
            userProfileEditService.updateProfile(userId, request.toCommand())
    );
  }

  @CheckNicknameAvailabilitySwagger
  @GetMapping("/me/nickname-availability")
  public BaseResponse<NicknameAvailabilityResponse> checkNicknameAvailability(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @RequestParam @NotBlank @Size(max = 14) String nickname
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        new NicknameAvailabilityResponse(
            userProfileEditService.isNicknameAvailable(userId, nickname)
        )
    );
  }

  @GetNotificationSettingSwagger
  @GetMapping("/me/notification-settings")
  public BaseResponse<NotificationSettingResponse> getNotificationSetting(
      @Parameter(hidden = true)
      @LoginUser Long userId
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        NotificationSettingResponse.of(userService.getNotificationSetting(userId))
    );
  }

  @UpdateNotificationSettingSwagger
  @PatchMapping("/me/notification-settings")
  public BaseResponse<NotificationSettingResponse> updateNotificationSetting(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @RequestBody @Valid UpdateNotificationSettingRequest request
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        NotificationSettingResponse.of(
            userService.updateNotificationSetting(userId, request.notificationEnabled())
        )
    );
  }

  @GetMyPostsSwagger
  @GetMapping("/me/posts")
  public BaseResponse<UserPostsResponse> getMyPosts(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "페이지 번호. 0 이상입니다.", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "페이지 크기. 1 이상 100 이하입니다.", example = "10")
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        UserPostsResponse.from(userService.getPosts(userId, page, size))
    );
  }

  @GetMyCoursesSwagger
  @GetMapping("/me/courses")
  public BaseResponse<UserCoursesResponse> getMyCourses(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "페이지 번호. 0 이상입니다.", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "페이지 크기. 1 이상 100 이하입니다.", example = "18")
      @RequestParam(defaultValue = "18") @Min(1) @Max(100) int size
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        UserCoursesResponse.from(userService.getCourses(userId, page, size))
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
        UserPublicProfileResponse.from(userService.getPublicProfile(userId))
    );
  }

  @GetUserPostsSwagger
  @GetMapping("/{userId}/posts")
  public BaseResponse<UserPostsResponse> getUserPosts(
      @Parameter(description = "조회할 사용자 ID", example = "1")
      @PathVariable Long userId,
      @Parameter(description = "페이지 번호. 0 이상입니다.", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "페이지 크기. 1 이상 100 이하입니다.", example = "10")
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        UserPostsResponse.from(userService.getPublicPosts(userId, page, size))
    );
  }

  @GetUserCoursesSwagger
  @GetMapping("/{userId}/courses")
  public BaseResponse<UserCoursesResponse> getUserCourses(
      @Parameter(description = "조회할 사용자 ID", example = "1")
      @PathVariable Long userId,
      @Parameter(description = "페이지 번호. 0 이상입니다.", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "페이지 크기. 1 이상 100 이하입니다.", example = "18")
      @RequestParam(defaultValue = "18") @Min(1) @Max(100) int size
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        UserCoursesResponse.from(userService.getPublicCourses(userId, page, size))
    );
  }

  @CompleteOnboardingSwagger
  @PatchMapping("/onboarding")
  public BaseResponse<OnboardingResponse> completeOnboarding(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @RequestBody @Valid OnboardingRequest request
  ) {
    User user = userOnboardingService.completeOnboarding(userId, request.toCommand());
    return BaseResponse.success(GlobalSuccessCode.OK, OnboardingResponse.of(user));
  }
}
