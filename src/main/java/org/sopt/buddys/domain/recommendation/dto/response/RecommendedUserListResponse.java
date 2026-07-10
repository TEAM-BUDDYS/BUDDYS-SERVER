package org.sopt.buddys.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.recommendation.service.result.RecommendedUserResult;

public record RecommendedUserListResponse(
    @Schema(description = "추천 사용자 리스트") List<RecommendedUserResponse> users
) {
  public static RecommendedUserListResponse from(List<RecommendedUserResult> results) {
    return new RecommendedUserListResponse(results.stream().map(RecommendedUserResponse::from).toList());
  }
}