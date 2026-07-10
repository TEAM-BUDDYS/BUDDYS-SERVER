package org.sopt.buddys.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.recommendation.service.result.RecommendedUserResult;

public record ExchangeCountryRecommendedUserListResponse(
    @Schema(description = "추천 사용자 목록")
    List<ExchangeCountryRecommendedUserResponse> users
) {

  public ExchangeCountryRecommendedUserListResponse {
    users = List.copyOf(users);
  }

  public static ExchangeCountryRecommendedUserListResponse from(List<RecommendedUserResult> results) {
    return new ExchangeCountryRecommendedUserListResponse(
        results.stream()
            .map(ExchangeCountryRecommendedUserResponse::from)
            .toList()
    );
  }
}