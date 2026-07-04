package org.sopt.buddys.domain.tag.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.tag.entity.Tag;

public record TagResponse(
    @Schema(description = "태그 ID", example = "1") Long id,
    @Schema(description = "태그 이름", example = "여행") String name
) {
  public static TagResponse from(Tag tag) {
    return new TagResponse(tag.getId(), tag.getName());
  }
}
