package org.sopt.buddys.domain.verification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ExchangeVerificationSubmitRequest(
    @Schema(description = "업로드 URL 발급 응답으로 받은 S3 객체 키")
    @NotBlank
    @Size(max = 512)
    String documentKey,

    @Schema(description = "사용자가 선택한 원본 파일명", example = "교환학생_입학허가서.pdf")
    @NotBlank
    @Size(max = 255)
    String originalFileName,

    @Schema(
        description = "업로드 URL 발급 시 사용한 Content-Type",
        example = "application/pdf",
        allowableValues = {"application/pdf", "image/jpeg", "image/png"}
    )
    @NotBlank
    String contentType,

    @Schema(description = "업로드한 파일 크기(byte)", example = "823044")
    @NotNull
    @Positive
    @Max(10 * 1024 * 1024)
    Long fileSize
) {
}
