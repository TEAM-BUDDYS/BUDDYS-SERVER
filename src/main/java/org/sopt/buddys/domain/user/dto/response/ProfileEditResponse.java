package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
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

    @Schema(description = "드래그앤드롭으로 지정한 순서대로 정렬된 전체 선택 태그. 앞 3개가 대표 태그")
    List<OrderedTagResponse> orderedTags
) {

  public ProfileEditResponse {
    orderedTags = List.copyOf(orderedTags);
  }

  public static ProfileEditResponse of(User user, List<UserTag> userTags) {
    List<OrderedTagResponse> orderedTags = userTags.stream()
        .map(OrderedTagResponse::from)
        .toList();
    return new ProfileEditResponse(
        user.getNickname(), user.getGender(), user.getBirthDate(), user.getIntroduction(),
        orderedTags
    );
  }
}
