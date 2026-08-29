package org.sopt.buddys.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.user.controller.swagger.CompleteOnboardingSwagger;
import org.sopt.buddys.domain.user.controller.swagger.GetMyPostsSwagger;
import org.sopt.buddys.domain.user.controller.swagger.GetMyProfileSwagger;
import org.sopt.buddys.domain.user.controller.swagger.GetUserPostsSwagger;
import org.sopt.buddys.domain.user.controller.swagger.GetUserProfileSwagger;
import org.sopt.buddys.domain.user.dto.request.OnboardingRequest;
import org.sopt.buddys.domain.user.dto.request.UpdateProfileRequest;
import org.sopt.buddys.domain.user.dto.response.NicknameAvailabilityResponse;
import org.sopt.buddys.domain.user.dto.response.OnboardingResponse;
import org.sopt.buddys.domain.user.dto.response.ProfileEditResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

  @Operation(summary = "프로필 편집 정보 조회", description = "편집 가능한 프로필 값과 현재 선택된 태그 ID를 반환합니다.")
  @GetMapping("/me/edit")
  public BaseResponse<ProfileEditResponse> getMyProfileForEdit(
          @Parameter(hidden = true)
          @LoginUser Long userId
  ) {
    return BaseResponse.success(GlobalSuccessCode.OK, userProfileEditService.getProfile(userId));
  }

  @Operation(summary = "프로필 수정", description = "닉네임, 성별, 생년월일, 자기소개와 카테고리별 태그를 수정합니다.")
  @PatchMapping("/me")
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

  @Operation(summary = "닉네임 중복 확인", description = "현재 사용자를 제외하고 닉네임 사용 가능 여부를 확인합니다.")
  @GetMapping("/me/nickname-availability")
  public BaseResponse<NicknameAvailabilityResponse> checkNicknameAvailability(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @RequestParam @NotBlank @Size(max = 50) String nickname
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        new NicknameAvailabilityResponse(
            userProfileEditService.isNicknameAvailable(userId, nickname)
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
