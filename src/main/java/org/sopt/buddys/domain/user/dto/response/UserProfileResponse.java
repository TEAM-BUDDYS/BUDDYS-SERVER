package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.entity.VerificationBadge;

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

  public static UserProfileResponse of(
      User user,
      List<String> representativeTags,
      List<TagGroupResponse> allTags
  ) {
    return new UserProfileResponse(
        user.getId(),
        user.getNickname(),
        user.getProfileImageUrl(),
        user.getIntroduction(),
        getVerificationBadge(user),
        List.copyOf(representativeTags),
        List.copyOf(allTags)
    );
  }

  private static VerificationBadge getVerificationBadge(User user) {
    if (user.isExchangeVerified()) {
      return VerificationBadge.EXCHANGE_VERIFIED;
    }
    if (user.isUniversityVerified()) {
      return VerificationBadge.UNIVERSITY_VERIFIED;
    }
    return VerificationBadge.SOCIAL_LOGIN;
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
  }
}
