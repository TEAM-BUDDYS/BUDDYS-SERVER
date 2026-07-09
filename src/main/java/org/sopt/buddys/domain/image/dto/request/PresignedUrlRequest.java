package org.sopt.buddys.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.sopt.buddys.domain.image.entity.ImageDomain;

public record PresignedUrlRequest(
    @Schema(description = "이미지가 사용될 도메인 (POST: 게시글, PROFILE: 프로필)", example = "POST")
    @NotNull
    ImageDomain imageDomain,

    @Schema(
        description = "업로드할 이미지의 Content-Type. image/jpeg, image/png, image/webp만 허용됩니다.",
        example = "image/jpeg",
        allowableValues = {"image/jpeg", "image/png", "image/webp"}
    )
    @NotBlank
    String contentType
) {
}