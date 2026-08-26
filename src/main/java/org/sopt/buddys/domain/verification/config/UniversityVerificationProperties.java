package org.sopt.buddys.domain.verification.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "verification.university-email")
public record UniversityVerificationProperties(
    @Min(1) long tokenExpiration,
    @NotBlank String confirmUrl
) {}
