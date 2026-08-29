package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.UserTag;
import org.sopt.buddys.domain.user.service.result.UserProfileResult;

public record OrderedTagResponse(
    @Schema(description = "태그 ID", example = "27")
    Long id,

    @Schema(description = "태그 이름", example = "계획형")
    String name,

    @Schema(description = "태그 카테고리", example = "TRAVEL_STYLE")
    TagType tagType
) {

  public static OrderedTagResponse from(UserProfileResult.OrderedTagResult result) {
    return new OrderedTagResponse(
        result.id(), result.name(), result.tagType()
    );
  }

  public static OrderedTagResponse from(UserTag userTag) {
    return new OrderedTagResponse(
        userTag.getTag().getId(),
        userTag.getTag().getName(),
        userTag.getTag().getTagType()
    );
  }
}
