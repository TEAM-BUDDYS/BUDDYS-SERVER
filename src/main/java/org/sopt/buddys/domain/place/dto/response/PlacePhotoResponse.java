package org.sopt.buddys.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.buddys.domain.place.service.result.PlacePhoto;
import org.sopt.buddys.domain.place.service.result.PlacePhotoSource;

public record PlacePhotoResponse(
    @Schema(description = "이미지 URL. img src에 그대로 사용합니다.",
        example = "https://images.unsplash.com/photo-1499856871958-5b9627545d1a?ixid=abc&w=400&fit=max&fm=jpg&q=80",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String url,

    @Schema(description = "사진 출처", example = "UNSPLASH", requiredMode = Schema.RequiredMode.REQUIRED)
    PlacePhotoSource source,

    @Schema(description = "작가 이름. UNSPLASH일 때만 값이 있고, 그 외에는 null입니다.", example = "Léonard Cotte",
        nullable = true)
    String authorName,

    @Schema(description = "작가 Unsplash 프로필 URL(utm 포함). UNSPLASH일 때만 값이 있고, 그 외에는 null입니다.",
        example = "https://unsplash.com/@ettocl?utm_source=buddys&utm_medium=referral", nullable = true)
    String authorPageUrl
) {

  public static PlacePhotoResponse from(PlacePhoto photo) {
    return new PlacePhotoResponse(photo.url(), photo.source(), photo.authorName(), photo.authorPageUrl());
  }
}
