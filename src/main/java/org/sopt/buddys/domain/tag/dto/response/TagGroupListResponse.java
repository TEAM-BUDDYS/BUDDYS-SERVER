package org.sopt.buddys.domain.tag.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.tag.service.result.TagGroupResult;

public record TagGroupListResponse(
    @Schema(
            description = "태그 카테고리",
            example = "ACTIVITY",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    TagType tagType,

    @Schema(
            description = "카테고리에 속한 전체 태그",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<TagResponse> tags
) {

  public TagGroupListResponse {
    tags = List.copyOf(tags);
  }

  public static TagGroupListResponse from(TagGroupResult result) {
    return new TagGroupListResponse(
        result.tagType(),
        result.tags().stream().map(TagResponse::from).toList()
    );
  }
}
