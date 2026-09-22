package org.sopt.buddys.domain.verification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ExchangeDocumentUploadUrlRequest(
    @Schema(
        description = "업로드할 서류의 Content-Type",
        example = "application/pdf",
        allowableValues = {"application/pdf", "image/jpeg", "image/png"}
    )
    @NotBlank
    String contentType,

    @Schema(
        description = "업로드할 파일 크기(byte). 최대 10MB까지 허용됩니다.",
        example = "823044"
    )
    @NotNull
    @Positive
    @Max(10 * 1024 * 1024)
    Long fileSize
) {
}
