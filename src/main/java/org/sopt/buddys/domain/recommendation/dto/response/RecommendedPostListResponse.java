package org.sopt.buddys.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.recommendation.service.result.RecommendedPostResult;

public record RecommendedPostListResponse(
    @Schema(description = "추천 게시글 리스트") List<RecommendedPostResponse> posts
) {
  public static RecommendedPostListResponse from(List<RecommendedPostResult> results) {
    return new RecommendedPostListResponse(results.stream().map(RecommendedPostResponse::from).toList());
  }
}