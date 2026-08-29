package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import org.sopt.buddys.domain.tag.dto.response.TagResponse;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.Gender;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.entity.UserTag;

public record ProfileEditResponse(
    @Schema(description = "닉네임", example = "정바미")
    String nickname,

    @Schema(description = "성별", example = "FEMALE")
    Gender gender,

    @Schema(description = "생년월일", example = "2004-10-24")
    LocalDate birthDate,

    @Schema(description = "자기소개", example = "안녕하세요 김버디입니다~~")
    String bio,

    @Schema(description = "현재 선택된 카테고리별 태그")
    List<SelectedTagGroupResponse> selectedTags
) {

  public ProfileEditResponse {
    selectedTags = List.copyOf(selectedTags);
  }

  public static ProfileEditResponse of(User user, List<UserTag> userTags) {
    List<SelectedTagGroupResponse> groups = List.of(
        toGroup(TagType.ACTIVITY, userTags),
        toGroup(TagType.INTEREST, userTags),
        toGroup(TagType.TRAVEL_STYLE, userTags)
    );
    return new ProfileEditResponse(
        user.getNickname(), user.getGender(), user.getBirthDate(), user.getIntroduction(), groups
    );
  }

  private static SelectedTagGroupResponse toGroup(TagType type, List<UserTag> userTags) {
    List<TagResponse> tags = userTags.stream()
        .map(UserTag::getTag)
        .filter(tag -> tag.getTagType() == type)
        .map(TagResponse::from)
        .toList();
    return new SelectedTagGroupResponse(type, tags);
  }

  public record SelectedTagGroupResponse(
      @Schema(description = "태그 카테고리", example = "ACTIVITY")
      TagType tagType,
      @Schema(description = "현재 선택된 태그")
      List<TagResponse> tags
  ) {
    public SelectedTagGroupResponse {
      tags = List.copyOf(tags);
    }
  }
}
