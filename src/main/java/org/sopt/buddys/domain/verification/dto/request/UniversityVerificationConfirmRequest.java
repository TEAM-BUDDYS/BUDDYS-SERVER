package org.sopt.buddys.domain.verification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UniversityVerificationConfirmRequest(
    @Schema(description = "이메일로 받은 인증 코드(영어 대문자 + 숫자 6자리)", example = "A1B2C3")
    @NotBlank
    @Pattern(regexp = "^[A-Z0-9]{6}$", message = "인증 코드는 영어 대문자와 숫자로 이루어진 6자리입니다.")
    String code
) {}
