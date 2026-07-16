package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.service.result.UserProfileResult;

public record TagGroupResponse(
    @Schema(description = "태그 타입", example = "ACTIVITY")
    TagType tagType,

    @Schema(description = "해당 타입의 전체 태그 이름 목록", example = "[\"액티비티\", \"맛집탐방\"]")
    List<String> tags
) {

  public TagGroupResponse {
    tags = List.copyOf(tags);
  }

  public static TagGroupResponse from(UserProfileResult.TagGroupResult result) {
    return new TagGroupResponse(result.tagType(), result.tags());
  }
}