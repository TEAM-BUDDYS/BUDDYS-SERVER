package org.sopt.buddys.domain.recommendation.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.recommendation.controller.swagger.GetRecommendedPostsSwagger;
import org.sopt.buddys.domain.recommendation.controller.swagger.GetRecommendedUsersSwagger;
import org.sopt.buddys.domain.recommendation.dto.response.RecommendedPostListResponse;
import org.sopt.buddys.domain.recommendation.dto.response.RecommendedUserListResponse;
import org.sopt.buddys.domain.recommendation.service.RecommendationService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/recommendations")
@Tag(name = "Recommendation", description = "추천 API")
public class RecommendationController {

  private static final int MAX_SIZE = 10;
  private final RecommendationService recommendationService;

  @GetRecommendedUsersSwagger
  @GetMapping("/users")
  public BaseResponse<RecommendedUserListResponse> getRecommendedUsers(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "조회할 개수. 1 이상 " + MAX_SIZE + "이하입니다.", example = "1")
      @RequestParam(defaultValue = "1") @Min(1) @Max(MAX_SIZE) int size
  ) {
    return BaseResponse.success(GlobalSuccessCode.OK,
        RecommendedUserListResponse.from(recommendationService.getRecommendedUsers(userId, size))
    );
  }

  @GetRecommendedPostsSwagger
  @GetMapping("/posts")
  public BaseResponse<RecommendedPostListResponse> getRecommendedPosts(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "조회할 개수. 1 이상 10 이하입니다.", example = "4")
      @RequestParam(defaultValue = "4") @Min(1) @Max(10) int size
  ) {
    return BaseResponse.success(GlobalSuccessCode.OK,
        RecommendedPostListResponse.from(recommendationService.getRecommendedPosts(userId, size)));
  }
}
