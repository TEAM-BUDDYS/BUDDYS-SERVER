package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.entity.VerificationBadge;
import org.sopt.buddys.domain.user.service.result.UserProfileResult;

public record UserProfileResponse(
    @Schema(description = "사용자 ID", example = "1")
    Long userId,

    @Schema(description = "닉네임", example = "버디")
    String nickname,

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.png")
    String profileImageUrl,

    @Schema(description = "자기소개", example = "같이 여행해요!")
    String bio,

    @Schema(description = "프로필에 표시할 인증 뱃지", example = "SOCIAL_LOGIN")
    VerificationBadge verificationBadge,

    @Schema(description = "사용자가 지정한 순서대로 정렬된 전체 취향 태그")
    List<OrderedTagResponse> orderedTags
) {

  public static UserProfileResponse from(UserProfileResult result) {
    User user = result.user();
    return new UserProfileResponse(
        user.getId(),
        user.getNickname(),
        user.getProfileImageUrl(),
        user.getIntroduction(),
        VerificationBadge.from(user),
        result.orderedTags().stream().map(OrderedTagResponse::from).toList()
    );
  }
}
