package org.sopt.buddys.domain.recommendation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.recommendation.dto.response.RecommendedUserListResponse;
import org.sopt.buddys.domain.recommendation.service.RecommendationService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.sopt.buddys.global.swagger.InvalidRequestResponse;
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

  private static final int DEFAULT_EXCHANGE_COUNTRY_USER_SIZE = 5;
  private static final int MAX_SIZE = 10;

  private final RecommendationService recommendationService;

  @Operation(
      summary = "같은 파견 국가 추천 사용자 조회",
      description = "로그인한 사용자와 같은 파견 국가를 가진 사용자 중 온보딩 태그 일치율이 높은 사용자를 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "400", description = "파견 국가 정보가 설정되어 있지 않음"),
      @ApiResponse(responseCode = "401", description = "인증 필요"),
      @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @GetMapping("/users/exchange-country")
  public BaseResponse<RecommendedUserListResponse> getExchangeCountryRecommendedUsers(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "조회할 개수. 1 이상 " + MAX_SIZE + " 이하입니다.", example = "5")
      @RequestParam(defaultValue = "" + DEFAULT_EXCHANGE_COUNTRY_USER_SIZE) @Min(1) @Max(MAX_SIZE) int size
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        RecommendedUserListResponse.from(recommendationService.getExchangeCountryRecommendedUsers(userId, size))
    );
  }
}
