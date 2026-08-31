package org.sopt.buddys.domain.verification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UniversityVerificationRequest(
    @Schema(description = "학교 이메일", example = "student@snu.ac.kr")
    @NotBlank @Email String email
) {}
