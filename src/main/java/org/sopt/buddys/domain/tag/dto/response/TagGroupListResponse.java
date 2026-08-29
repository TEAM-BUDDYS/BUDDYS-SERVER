package org.sopt.buddys.domain.tag.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.sopt.buddys.domain.tag.entity.Tag;
import org.sopt.buddys.domain.tag.entity.TagType;

public record TagGroupListResponse(
    @Schema(description = "태그 카테고리", example = "ACTIVITY")
    TagType tagType,

    @Schema(description = "카테고리에 속한 전체 태그")
    List<TagResponse> tags
) {

  public TagGroupListResponse {
    tags = List.copyOf(tags);
  }

  public static TagGroupListResponse of(TagType tagType, List<Tag> tags) {
    return new TagGroupListResponse(tagType, tags.stream().map(TagResponse::from).toList());
  }
}
