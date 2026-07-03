package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.tag.entity.TagType;
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

    @Schema(description = "대표 취향 태그", example = "[\"문화생활\", \"액티비티\", \"활발한\"]")
    List<String> representativeTags,

    @Schema(description = "전체 취향 태그")
    List<TagGroupResponse> allTags
) {

  public static UserProfileResponse from(UserProfileResult result) {
    User user = result.user();
    return new UserProfileResponse(
        user.getId(),
        user.getNickname(),
        user.getProfileImageUrl(),
        user.getIntroduction(),
        VerificationBadge.from(user),
        result.representativeTags(),
        result.allTags()
            .stream()
            .map(TagGroupResponse::from)
            .toList()
    );
  }

  public record TagGroupResponse(
      @Schema(description = "태그 타입", example = "ACTIVITY")
      TagType tagType,

      @Schema(description = "해당 타입의 전체 태그 이름 목록", example = "[\"액티비티\", \"맛집탐방\"]")
      List<String> tags
  ) {

    public TagGroupResponse {
      tags = List.copyOf(tags);
    }

    private static TagGroupResponse from(UserProfileResult.TagGroupResult result) {
      return new TagGroupResponse(result.tagType(), result.tags());
    }
  }
}
