package org.sopt.buddys.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.recommendation.service.result.RecommendedUserResult;

public record RecommendedUserResponse(
    @Schema(description = "사용자 ID", example = "1") Long userId,
    @Schema(description = "사용자 닉네임", example = "버디즈") String nickname,
    @Schema(description = "프로필 이미지 주소") String profileImageUrl,
    @Schema(description = "유사도") int similarityScore
) {
  public static RecommendedUserResponse from(RecommendedUserResult result) {
    return new RecommendedUserResponse(
        result.user().getId(), result.user().getNickname(), result.user().getProfileImageUrl(),
        (int) Math.round(result.totalSimilarity() * 100)
    );
  }
}