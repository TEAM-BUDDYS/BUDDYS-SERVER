package org.sopt.buddys.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.service.result.UserProfileResult;

public record UserPublicProfileResponse(
    @Schema(description = "사용자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long userId,

    @Schema(description = "닉네임", example = "버디", requiredMode = Schema.RequiredMode.REQUIRED)
    String nickname,

    @Schema(
        description = "프로필 이미지 URL",
        example = "https://example.com/profile.png",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String profileImageUrl,

    @Schema(description = "자기소개", example = "같이 여행해요!", requiredMode = Schema.RequiredMode.REQUIRED)
    String bio,

    @Schema(
        description = "학교 이메일 인증 여부",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean universityEmailVerified,

    @Schema(
        description = "파견교 서류 인증 여부",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean exchangeDocumentVerified,

    @Schema(
        description = "사용자가 지정한 순서의 상위 3개 대표 취향 태그",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<OrderedTagResponse> representativeTags,

    @JsonProperty("isDeleted")
    @Schema(
        description = "삭제된 사용자 여부",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean deleted
) {

  public UserPublicProfileResponse {
    representativeTags = List.copyOf(representativeTags);
  }

  public static UserPublicProfileResponse from(UserProfileResult result) {
    User user = result.user();
    return new UserPublicProfileResponse(
        user.getId(),
        user.getDisplayNickname(),
        user.getProfileImageUrl(),
        user.getIntroduction(),
        user.isUniversityVerified(),
        user.isExchangeVerified(),
        result.orderedTags().stream()
            .limit(3)
            .map(OrderedTagResponse::from)
            .toList(),
        user.getDeletedAt() != null
    );
  }
}
