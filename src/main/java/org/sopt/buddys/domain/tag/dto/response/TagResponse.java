package org.sopt.buddys.domain.tag.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.tag.entity.Tag;
import org.sopt.buddys.domain.tag.service.result.TagGroupResult.TagResult;

public record TagResponse(
    @Schema(description = "태그 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long id,
    @Schema(description = "태그 이름", example = "여행", requiredMode = Schema.RequiredMode.REQUIRED)
    String name
) {
  public static TagResponse from(Tag tag) {
    return new TagResponse(tag.getId(), tag.getName());
  }

  public static TagResponse from(TagResult result) {
    return new TagResponse(result.id(), result.name());
  }
}
